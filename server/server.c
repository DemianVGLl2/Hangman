// Compilacion: cc server.c -lnsl -o server
// Ejecución: ./server

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netdb.h>
#include <signal.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/time.h>

#define MAX_LENGTH 100
#define FILE_NAME "users.txt"
#define msgSIZE 1000
#define PUERTO 5000
#define TIMEOUT_SEC 10 // Tiempo de espera en segundos
#define MAX_PLAYERS 2  // Solo 2 jugadores

// Variable global para los sockets de ambos jugadores
int player_sockets[MAX_PLAYERS] = {-1, -1};
int player_count = 0;
int current_player = 0; // 0 = primer jugador (propone palabra), 1 = segundo jugador (adivina)

// Variables para el juego
char word[MAX_LENGTH] = "";             // Palabra a adivinar
char revealed[MAX_LENGTH] = "";         // Palabra parcialmente revelada
int attempts = 0;                       // Intentos fallidos
int game_active = 0;                    // Si hay un juego activo

int validateUser(const char *username, const char *password){
  char fileUser[MAX_LENGTH], filePass[MAX_LENGTH];
  FILE *file = fopen(FILE_NAME, "r");
  if (!file) {
    perror("Error al abrir el archivo");
    return 0;
  }
  int found = 0;
  while (fgets(fileUser, MAX_LENGTH, file) && fgets(filePass, MAX_LENGTH, file)) {
    fileUser[strcspn(fileUser, "\n")] = 0;
    filePass[strcspn(filePass, "\n")] = 0;
    if (strcmp(username, fileUser) == 0 && strcmp(password, filePass) == 0) {
      found = 1;
      break;
    }
  }
  fclose(file);
  return found;
}

void addUser(const char *username, const char *password){
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

char* processMessage(int socket, char *msg) {
  static char response[msgSIZE];

  if (strcmp(msg, "close") == 0) return "close";

  char *command = strtok(msg, ".");
  if (!command) return "error.format_incorrect";

  //Inicio de sesión
  if (strcmp(command, "1") == 0) {
    char *username = strtok(NULL, ".");
    char *password = strtok(NULL, ".");

    if (!username || !password) return "error.format_incorrect";

    if (validateUser(username, password)) {
      if (player_count < MAX_PLAYERS) {
        player_sockets[player_count] = socket;
        player_count++;

        if (player_count == 1) {
          sprintf(response, "login.ok.role.1"); //Proponer palabra
        } else if (player_count == 2) {
          sprintf(response, "login.ok.role.2"); //Adivina palabra

          char notify[msgSIZE];
          sprintf(notify, "game.start.your_turn");
          send(player_sockets[0],notify,strlen(notify),0);
        }
      } else {
        sprintf(response, "login.fail.server_full");
      }
    } else {
      sprintf(response, "login.fail.invalid_credentials");
    }
  }
  //Registrar usuario
  else if (strcmp(command, "2")) {
    char *username = strtok(NULL, ".");
    char *password = strtok(NULL, ".");
    if (!username || !password) {
      return "error.formato_incorrecto";
    }
    
    if (!validateUser(username, password)) {
      addUser(username, password);
      sprintf(response, "register.ok");
    } else {
      sprintf(response, "register.fail.user_exists");
    }
  }
  //Crear palabra
  else if (strcmp(command, "3")) {
    char *new_word = strtok(NULL, ".");
    if (!new_word) return "error.formato_incorrecto";

    if (socket == player_sockets[0]) {
      strcpy(word, new_word);

      memset(revealed, '_', strlen(word));
      for (int i=0; i<strlen(word); ) if (word[i] == ' ') revealed[i] = ' ';
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
  //Adivinar palabra
  else if (strcmp(command, "4")) {
    char *letter_str = strtok(NULL, ".");
    if (!letter_str || strlen(letter_str) != 1) return "error.format_incorrect";

    char letter = letter_str[0];

    if (socket == player_sockets[1] && game_active) {
      int found = 0;

      for (int i=0; i < strlen(word); i++) {
        if (word[i] == letter) {
          found = 1;
        }
      }

      if (found) {
        if (strcmp(word, revealed) == 0) {
          sprintf(response, "guess.win.%s", word);
          
          char notify[msgSIZE];
          sprintf(notify, "game.over.win");
          send(player_sockets[0], notify. strlen(notify), 0);

          game_active = 0;
        } else {
          sprintf(response, "guess.%c.1.%s", letter, revealed);

          char notify[msgSIZE];
          sprintf(notify, "progress.%s.%c", revealed, letter);
          send(player_sockets[0], notify, strlen(notify), 0);
        }
      } else {
        attempts++;
        if (attempts >=6) {
          sprintf(response, "guess.lose.%s", word);

          char notify[msgSIZE];
          sprintf(notify, "game.over.lose");
          send(player_sockets[0], notify, strlen(notify), 0);
          
          game_active = 0;
        }
      }
    } else sprintf(response, "guess.fail.not_allowed");
  }
  //Reiniciar
  else if (strcmp(command, "5") == 0) {
    if (socket == player_sockets[0] && !game_active) {
      memset(word, 0, sizeof(word));
      memset(revealed, 0, sizeof(revealed));
      attempts = 0;

      char notify[msgSIZE];
      sprintf(notify, "game.restart");
      send(player_sockets[0], notify, strlen(notify), 0);
      send(player_sockets[1], notify, strlen(notify), 0);
      
      sprintf(response, "restart.ok");
    } else sprintf(response, "restart.fail");
  } else sprintf(response, "error.comando_desconocido");
  return response;
}

void aborta_handler(int sig){
  printf("....abortando el proceso servidor %d\n",sig);
  exit(1);
}

void handle_disconnect(int socket_index) {
  if (socket_index >= 0 && socket_index < MAX_PLAYERS) {
    player_sockets[socket_index] = -1;
    player_count--;

    for (int i=0; i < MAX_PLAYERS; i++) {
      if (player_sockets[i] != -1) {
        char notify[msgSIZE];
        sprintf(notify, "opponent.disconnected");
        send(player_sockets[i], notify, strlen(notify), 0);
      }
    }

    game_active = 0;
    memset(word, 0, sizeof(word));
    memset(revealed, 0, sizeof(revealed));
    attempts = 0;
  }
}

int main() {
  int sd, sd_actual;
  struct sockaddr_in sind, pin;
  char msg[msgSIZE];

  if(signal(SIGINT, aborta_handler) == SIG_ERR){
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

  printf("Servidor iniciado en el puerto %d. Esperando conexiones...\n", PUERTO);
  fflush(stdout);

  while(1) {
    socklen_t addrlen = sizeof(pin);
    sd_actual = accept(sd, (struct sockaddr *)&pin, &addrlen);
    if (sd_actual == -1) {
      perror("accept");
      continue;
    }

    struct timeval timeout;
    timeout.tv_sec = TIMEOUT_SEC;
    timeout.tv_usec = 0;
    setsockopt(sd_actual, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));

    pid_t pid = fork();
    if (pid < 0) {
      perror("fork");
      close(sd_actual);
      continue;
    }
    if (pid == 0) { 
      close(sd);
      char sigue = 'S';
      int player_index = -1;

      for (int i=0; i<MAX_PLAYERS; i++) {
        if (player_sockets[i] == sd_actual) {
          player_index = i;
          break;
        }
      }

      while(sigue == 'S') { 
        int n = recv(sd_actual, msg, sizeof(msg) - 1, 0);
        if (n <= 0) {
          perror("recv (timeout or error)");
          break;
        }
        msg[n] = '\0';
        printf("Cliente (PID %d) envió: %s\n", (int)getpid(), msg);
        fflush(stdout);

        char *response = processMessage(sd_actual, msg);

        if(send(sd_actual, response, strlen(response), 0) == -1) {
          perror("send response");
        } else {
          printf("Enviado a cliente: %s\n", response);
          fflush(stdout);
        }
        
        if(strcmp(msg, "close") == 0 || strcmp(response, "close") == 0) {
          sigue = 'N';
          handle_disconnect(player_index);
        }
      }
      close(sd_actual);
      printf("Conexión cerrada en proceso hijo (PID %d).\n", (int)getpid());
      fflush(stdout);
      exit(0);
    } else {
      close(sd_actual);
    }
  }
  close(sd);
  return 0;
}