// Compilación: cc server_threads.c -lpthread -lnsl -o server
// Ejecución: ./server

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netdb.h>
#include <netinet/tcp.h>
#include <signal.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/time.h>
#include <arpa/inet.h>
#include <pthread.h>
#include <errno.h>

#define MAX_LENGTH 100
#define FILE_NAME "users.txt"
#define msgSIZE 1000
#define PUERTO 5000
#define TIMEOUT_SEC 200  // Tiempo de espera en segundos
#define MAX_PLAYERS 2    // Solo 2 jugadores

// Variables globales para el juego
char word[MAX_LENGTH] = "";       // Palabra a adivinar
char revealed[MAX_LENGTH] = "";   // Palabra parcialmente revelada
int attempts = 0;                 // Intentos fallidos
int game_active = 0;              // Si hay un juego activo
int game_rounds = 0;              // Contador de rondas para alternar roles

// Variables globales para los jugadores
int player_sockets[MAX_PLAYERS] = {-1, -1};
int player_count = 0;
int current_player = 0;  // 0 = primer jugador (propondrá la palabra), 1 = segundo jugador (adivina)

// Mutex para sincronizar la asignación de roles y actualizaciones globales
pthread_mutex_t role_mutex = PTHREAD_MUTEX_INITIALIZER;

/* Función para validar usuario */
int validateUser(const char *username, const char *password) {
    char fileUser[MAX_LENGTH], filePass[MAX_LENGTH];
    FILE *file = fopen(FILE_NAME, "r");
    if (!file) {
        perror("Error al abrir el archivo");
        return 0;
    }
    int found = 0;
    while (fgets(fileUser, MAX_LENGTH, file) && fgets(filePass, MAX_LENGTH, file)) {
        fileUser[strcspn(fileUser, "\r\n")] = '\0';
        filePass[strcspn(filePass, "\r\n")] = '\0';
        
        if (strcmp(username, fileUser) == 0 && strcmp(password, filePass) == 0) {
            found = 1;
            break;
        }
    }
    fclose(file);
    return found;
}

/* Función para agregar usuario */
void addUser(const char *username, const char *password) {
    if (validateUser(username, password)) {
        printf("El usuario ya existe. No se puede agregar nuevamente.\n");
        return;
    }
    FILE *file = fopen(FILE_NAME, "a");
    if (!file) {
        perror("Error al abrir el archivo");
        return;
    }
    fprintf(file, "%s\n%s\n", username, password);
    fclose(file);
    printf("Usuario agregado exitosamente.\n");
}

/* Función para encontrar el índice de un socket en el array de jugadores */
int findSocketIndex(int socket) {
    for (int i = 0; i < MAX_PLAYERS; i++) {
        if (player_sockets[i] == socket)
            return i;
    }
    return -1;
}

/* Función que procesa los mensajes recibidos y retorna la respuesta a enviar */
char* processMessage(int socket, char *msg, int *player_role) {
    static char response[msgSIZE];

    if (strcmp(msg, "close") == 0)
        return "close";

    char *command = strtok(msg, ".");
    if (!command)
        return "error.format_incorrect";

    // Inicio de sesión (regla 1)
    if (strcmp(command, "1") == 0) {
        char *username = strtok(NULL, ".");
        char *password = strtok(NULL, ".");
        if (!username || !password)
            return "error.format_incorrect";
        
        if (validateUser(username, password)) {
            printf("Usuario validado: %s\n", username);
            
            pthread_mutex_lock(&role_mutex);
            
            // Verificar si este socket ya está registrado
            int socket_index = findSocketIndex(socket);
            
            if (socket_index != -1) {
                // Este socket ya está registrado
                sprintf(response, "login.already");
                *player_role = socket_index;
            } else if (player_count < MAX_PLAYERS) {
                // Nuevo jugador
                socket_index = player_count;
                player_sockets[player_count] = socket;
                player_count++;
                *player_role = socket_index;
                
                sprintf(response, "login.ok.role.%d", socket_index + 1);
                
                // Si ahora hay dos jugadores, notificar al primer jugador
                if (player_count == 2) {
                    char notify[msgSIZE];
                    sprintf(notify, "game.start.your_turn");
                    send(player_sockets[0], notify, strlen(notify), 0);
                }
            } else {
                sprintf(response, "login.fail.server_full");
            }
            
            pthread_mutex_unlock(&role_mutex);
        } else {
            sprintf(response, "login.fail.invalid_credentials");
        }
    }
    // Registro (regla 2)
    else if (strcmp(command, "2") == 0) {
        printf("COMMAND2: %s\n", command);
        char *username = strtok(NULL, ".");
        char *password = strtok(NULL, ".");
        if (!username || !password)
            return "error.format_incorrect";
        if (!validateUser(username, password)) {
            addUser(username, password);
            sprintf(response, "2.ok");
        } else {
            sprintf(response, "register.fail.user_exists");
        }
    }
    // Establecer palabra secreta (regla 3)
    else if (strcmp(command, "3") == 0) {
        printf("COMMAND3: %s\n", command);
        char *new_word = strtok(NULL, ".");
        if (!new_word)
            return "error.format_incorrect";
        // Solo permite que el primer jugador establezca la palabra
        if (socket == player_sockets[current_player]) {
            strcpy(word, new_word);
            memset(revealed, '_', strlen(word));
            for (int i = 0; i < strlen(word); i++) {
                if (word[i] == ' ')
                    revealed[i] = ' ';
            }
            revealed[strlen(word)] = '\0';
            game_active = 1;
            attempts = 0;
            char notify[msgSIZE];
            sprintf(notify, "word.set.%s", revealed);
            send(player_sockets[1], notify, strlen(notify), 0);
            sprintf(response, "word.ok");
        } else {
            sprintf(response, "word.fail.not_allowed");
        }
    }
    // Intento de adivinanza (regla 4)
    else if (strcmp(command, "4") == 0) {
        char *letter_str = strtok(NULL, ".");
        if (!letter_str || strlen(letter_str) != 1)
            return "error.format_incorrect";
        char letter = letter_str[0];
        if (socket == player_sockets[(current_player + 1) % 2] && game_active) {
            int found = 0;
            for (int i = 0; i < strlen(word); i++) {
                if (word[i] == letter) {
                    found = 1;
                    revealed[i] = letter;
                }
            }
            if (found) {
                if (strcmp(word, revealed) == 0) {
                    sprintf(response, "guess.win.%s", word);
                    char notify[msgSIZE];
                    sprintf(notify, "game.over.win");
                    send(player_sockets[current_player], notify, strlen(notify), 0);
                    game_active = 0;
                } else {
                    sprintf(response, "guess.%c.1.%s", letter, revealed);
                    char notify[msgSIZE];
                    sprintf(notify, "progress.%s.%c", revealed, letter);
                    send(player_sockets[current_player], notify, strlen(notify), 0);
                }
            } else {
                attempts++;
                if (attempts >= 6) {
                    sprintf(response, "guess.lose.%s", word);
                    char notify[msgSIZE];
                    sprintf(notify, "game.over.lose");
                    send(player_sockets[current_player], notify, strlen(notify), 0);
                    game_active = 0;
                } else {
                    sprintf(response, "guess.%c.0.%s", letter, revealed);
                    char notify[msgSIZE];
                    sprintf(notify, "progress.%s.%c", revealed, letter);
                    send(player_sockets[current_player], notify, strlen(notify), 0);
                }
            }
        } else {
            sprintf(response, "guess.fail.not_allowed");
        }
    }
    // Intento de adivinar la palabra completa (regla 5)
    else if (strcmp(command, "5") == 0) {
        char *guessedWord = strtok(NULL, ".");
        if (!guessedWord)
            return "error.format_incorrect";
        // Comparar la palabra adivinada completa con la palabra original
        if (strcmp(guessedWord, word) == 0) {
            sprintf(response, "guess.win.%s", word);
            char notify[msgSIZE];
            sprintf(notify, "game.over.win");
            send(player_sockets[0], notify, strlen(notify), 0);
            game_active = 0;
        } else {
            sprintf(response, "guess.lose.%s", word);
            char notify[msgSIZE];
            sprintf(notify, "game.over.lose");
            send(player_sockets[0], notify, strlen(notify), 0);
            game_active = 0;
        }
    }
    // Reiniciar juego (regla 6)
    else if (strcmp(command, "6") == 0) {
        printf("Comando 6 recibido. game_active = %d, socket = %d, player_sockets[0] = %d\n", game_active, socket, player_sockets[0]);
        // Permite reiniciar si no hay juego activo
        if (game_active == 0) {
            memset(word, 0, sizeof(word));
            memset(revealed, 0, sizeof(revealed));
            attempts = 0;

            game_rounds++;
            current_player = game_rounds % 2;

            char notify[msgSIZE];
            sprintf(notify, "game.restart.role.%d", 1); // Rol 1 para el jugador ahora en posición 0
            send(player_sockets[0], notify, strlen(notify), 0);
            
            sprintf(notify, "game.restart.role.%d", 2); // Rol 2 para el jugador ahora en posición 1
            send(player_sockets[1], notify, strlen(notify), 0);
            
            //sprintf(response, "restart.ok");
        } else {
            sprintf(response, "restart.fail");
        }
    }
    else {
        sprintf(response, "error.comando_desconocido");
    }
    return response;
}

/* Manejador de señales para abortar el servidor */
void aborta_handler(int sig) {
    printf("....abortando el proceso servidor %d\n", sig);
    exit(1);
}

/* Maneja la desconexión de un jugador */
void handle_disconnect(int socket_index) {
    if (socket_index >= 0 && socket_index < MAX_PLAYERS) {
        pthread_mutex_lock(&role_mutex);
        
        // Notificar al otro jugador que su oponente se desconectó
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (i != socket_index && player_sockets[i] != -1) {
                char notify[msgSIZE];
                sprintf(notify, "opponent.disconnected");
                send(player_sockets[i], notify, strlen(notify), 0);
            }
        }
        
        // Marcar el socket como desconectado
        player_sockets[socket_index] = -1;
        
        // Si ahora no hay suficientes jugadores, reiniciar el juego
        if (game_active) {
            game_active = 0;
            memset(word, 0, sizeof(word));
            memset(revealed, 0, sizeof(revealed));
            attempts = 0;
        }
        
        // Reajustar la lista de jugadores si es necesario
        // Nota: podríamos mejorar esto para mover el jugador 1 al slot 0 si el jugador 0 se desconecta
        if (socket_index == 0 && player_sockets[1] != -1) {
            player_sockets[0] = player_sockets[1];
            player_sockets[1] = -1;
            player_count = 1;
        } else {
            player_count--;
            if (player_count < 0) player_count = 0;
        }
        
        pthread_mutex_unlock(&role_mutex);
    }
}

/* ==================== VERSIÓN CON THREADS ==================== */
typedef struct {
  int sd_actual;
} client_data_t;

void *clientHandler(void *arg) {
  client_data_t *data = (client_data_t *) arg;
  int sd_actual = data->sd_actual;
  free(data);

  int my_role = -1; // Se asignará cuando el usuario se autentique

  char msg[msgSIZE];
  char sigue = 'S';

  while (sigue == 'S') {
      int n = recv(sd_actual, msg, sizeof(msg) - 1, 0);
      if (n <= 0) {
          if (errno == EAGAIN || errno == EWOULDBLOCK) {
              printf("Timeout alcanzado para el socket %d\n", sd_actual);
          } else {
              perror("Error en recv");
          }
          break;
      }
      msg[n] = '\0';
      printf("Cliente (socket %d) envió: %s\n", sd_actual, msg);
      fflush(stdout);

      char *response = processMessage(sd_actual, msg, &my_role);
      if (send(sd_actual, response, strlen(response), 0) == -1) {
          perror("Error en send");
          break;
      } else {
          printf("Enviado a cliente (rol %d): %s\n", my_role + 1, response);
          fflush(stdout);
      }

      if (strcmp(msg, "close") == 0 || strcmp(response, "close") == 0) {
          sigue = 'N';
      }
  }
  
  // Manejar la desconexión del cliente
  printf("Cliente desconectado (socket %d, rol %d)\n", sd_actual, my_role + 1);
  if (my_role >= 0) {
      handle_disconnect(my_role);
  }
  
  close(sd_actual);
  pthread_exit(NULL);
}

int main() {
  int sd;
  struct sockaddr_in sind, pin;
  int addrlen;
  
  if (signal(SIGINT, aborta_handler) == SIG_ERR) {
      perror("Could not set signal handler");
      return 1;
  }
  
  if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
      perror("socket");
      exit(1);
  }
  
  sind.sin_family = AF_INET;
  sind.sin_addr.s_addr = INADDR_ANY;
  sind.sin_port = htons(PUERTO);
  
  int opt = 1;
  if (setsockopt(sd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
      perror("setsockopt");
      exit(1);
  }
  
  if (bind(sd, (struct sockaddr *)&sind, sizeof(sind)) == -1) {
      perror("bind");
      exit(1);
  }
  
  if (listen(sd, 5) == -1) {
      perror("listen");
      exit(1);
  }
  
  printf("Servidor (threads) iniciado en el puerto %d. Esperando conexiones...\n", PUERTO);
  fflush(stdout);
  
  while (1) {
      addrlen = sizeof(pin);
      int sd_actual = accept(sd, (struct sockaddr *)&pin, &addrlen);
      if (sd_actual == -1) {
          perror("accept");
          continue;
      }
      
      struct timeval timeout;
      timeout.tv_sec = TIMEOUT_SEC;
      timeout.tv_usec = 0;
      setsockopt(sd_actual, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
      
      int flag = 1;
      if (setsockopt(sd_actual, IPPROTO_TCP, TCP_NODELAY, &flag, sizeof(flag)) < 0) {
          perror("setsockopt TCP_NODELAY");
      }
      
      client_data_t *data = malloc(sizeof(client_data_t));
      if (data == NULL) {
          perror("malloc");
          close(sd_actual);
          continue;
      }
      
      data->sd_actual = sd_actual;
      
      pthread_t tid;
      if (pthread_create(&tid, NULL, clientHandler, data) != 0) {
          perror("pthread_create");
          free(data);
          close(sd_actual);
          continue;
      }
      
      pthread_detach(tid);
  }
  
  close(sd);
  return 0;
}