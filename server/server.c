// Compilacion: cc tcpserver.c -lnsl -o tcpserver
// Ejecución: ./tcpserver

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netdb.h>
#include <signal.h>	
#include <unistd.h>

#define MAX_LENGTH 100
#define FILE_NAME "users.txt"
#define  msgSIZE   1000     
#define  PUERTO    5000	 


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
    //printf("Validado %d\n", found);
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

  // printf("✅ Usuario agregado exitosamente.\n");
}

void aborta_handler(int sig){
  printf("....abortando el proceso servidor %d\n",sig);
  exit(1);
}

void setWord(){

}
void guesseLetter(){

}

int main() {
  int sd, sd_actual;
  int addrlen;
  struct sockaddr_in sind, pin;
  char  msg[msgSIZE];	     /* parametro entrada y salida */
	char  action[msgSIZE];

  if(signal(SIGINT, aborta_handler) == SIG_ERR){
    perror("Could not set signal handler");
     return 1;
  }

  if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
    perror("socket");
    exit(1);
  }
  
  sind.sin_family = AF_INET;
  sind.sin_addr.s_addr = INADDR_ANY;   /* INADDR_ANY=0x000000 = yo mismo */
  sind.sin_port = htons(PUERTO);    
  
  if (bind(sd, (struct sockaddr *)&sind, sizeof(sind)) == -1) {
    perror("bind");
    exit(1);
  }

  if (listen(sd, 5) == -1) {
    perror("listen");
    exit(1);
  }

  printf("Servidor iniciado en el puerto %d. Esperando conexiones...\n", PUERTO);

  if ((sd_actual = accept(sd, (struct sockaddr *)&pin, &addrlen)) == -1) {
		perror("accept");
		exit(1);
	}

  // Bucle principal: aceptar conexiones y crear un proceso hijo para cada cliente
  while(1) {
    addrlen = sizeof(pin);
    sd_actual = accept(sd, (struct sockaddr *)&pin, &addrlen);
    if (sd_actual == -1) {
      perror("accept");
      continue;
    }

    pid_t pid = fork();
    if (pid < 0) {
      perror("fork");
      close(sd_actual);
      continue;
    }
    if (pid == 0) { //Proceso del hijo
      close(sd); //No necesita el socket de escucha
      strcpy(action, " ");
      char sigue = 'S';
      char *display;
    
      while(sigue=='S'){				
        /* tomar un mensaje del cliente */
        int n = recv(sd_actual, msg, sizeof(msg), 0);
        if (n == -1) {
          perror("recv");
          exit(1);
        }		
        msg[n] = '\0';		
        printf("Client (}PID %d) envió: %s", (int)getpid(), msg);
        
        if((strcmp(msg,"close")==0)){ //it means that the conversation must be closed
          sigue='N';
          strcpy(action,"close");
        } else {
          //convert msg received to action format
          
          char temp[msgSIZE];
          strcpy(temp, msg);
          char *token = strtok(temp, ".");
          int rule = atoi(token);
          
          switch (rule) {
            case 1:
              char *name1 = strtok(NULL, ".");
              char *pass1 = strtok(NULL, ".");
              if (name1 == NULL || pass1 == NULL){
                printf("error");
                return 1;
              }
              validateUser(name1, pass1);
              break;
              
            case 2:
              char *name2 = strtok(NULL, ".");
              char *pass2 = strtok(NULL, ".");
              if (name2 == NULL || pass2 == NULL){
                printf("error");
                return 1;
              }
              addUser(name2, pass2);
              break;

            case 3:
              char *word = strtok(NULL, ".");
              if (word) {
                printf("Palabra secreta recibida: %s\n", word);
                int len = strlen(word);
                for (int i=0; i<len; i++) {
                  if (word[i] != ' ') display[i] = '_';
                  else display[i] = ' ';
                }
                display[len] = '\0';
                snprintf(action, sizeof(action), "3.%s", display);
              }
              break;

            case 4:
              char *guess = strtok(NULL, ".");
              if (guess) {
                //Prueba de respuesta
                printf("Intento recibido: %s\n", guess);
                //Caso de éxito
                snprintf(action, sizeof(action), "4.%s.%d", display, 1);
              }
              break;

            default:
              printf("No se pudo\n");
            
          }
        }
      }
        close(sd_actual);
        printf("Conexión cerrada en proceso hijo (PID %d).\n", (int)getpid());
        exit(0);
    } else {
      // Proceso padre: cierra el descriptor del cliente y sigue aceptando conexiones
      close(sd_actual);
    }
  }
  close(sd);
  return 0;
}