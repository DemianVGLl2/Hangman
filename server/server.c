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
    printf("validado %d\n", found);
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

  // printf("✅ Usuario agregado exitosamente.\n");
}

void setWord(){

}
void guesseLetter(){

}

int                  sd, sd_actual; 
int                  addrlen;    
struct sockaddr_in   sind, pin;  


void aborta_handler(int sig){
  printf("....abortando el proceso servidor %d\n",sig);
  close(sd);  
  close(sd_actual); 
  exit(1);
}

int main(){
  char  msg[msgSIZE];	     /* parametro entrada y salida */
	char  json[msgSIZE];	 

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
  if ((sd_actual = accept(sd, (struct sockaddr *)&pin, &addrlen)) == -1) {
		perror("accept");
		exit(1);
	}
	char sigue='S';
	char msgReceived[1000];
	strcpy(json," ");
	while(sigue=='S'){				
		/* tomar un mensaje del cliente */
		int n = recv(sd_actual, msg, sizeof(msg), 0);
		if (n == -1) {
			perror("recv");
			exit(1);
		}		
		msg[n] = '\0';		
		//printf("Client sent: %d caracteres", n);
		printf("Client sent: %s\n", msg);
		if((strcmp(msg,"close")==0)){ //it means that the conversation must be closed
			sigue='N';
			strcpy(json,"close");
		}else{
			//convert msg received to json format
      
      char temp[1000]; 
      strcpy(temp, msg);

      char *token = strtok(temp, ".");
      if (token == NULL) {
          printf("Mensaje inválido.\n");
      }

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

        default:
          printf("No se pudo\n");
        } 

			//----------------------------------
		}		
		/* enviando la respuesta del servicio */
		int sent;
		if ( sent = send(sd_actual, json, strlen(json), 0) == -1) {
			perror("send");
			exit(1);
		}
	}

/* cerrar los dos sockets */
	close(sd_actual);  
   close(sd);
   printf("Conexion cerrada\n");
	return 0;



}