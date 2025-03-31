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

void aborta_handler(int sig){
  printf("....abortando el proceso servidor %d\n",sig);
  exit(1);
}

int main() {
  int sd, sd_actual;
  struct sockaddr_in sind, pin;
  char msg[msgSIZE];
  char action[msgSIZE];

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
      while(sigue == 'S') { 
        int n = recv(sd_actual, msg, sizeof(msg) - 1, 0);
        if (n <= 0) {
          perror("recv (timeout or error)");
          break;
        }
        msg[n] = '\0';
        printf("Cliente (PID %d) envió: %s\n", (int)getpid(), msg);
        fflush(stdout);

        if(send(sd_actual, "ack", 3, 0) == -1) {
          perror("send ack");
        } else {
          printf("Enviado: ack\n");
          fflush(stdout);
        }
        
        if(strcmp(msg,"close") == 0) {
          sigue = 'N';
          strcpy(action,"close");
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