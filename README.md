# Hangman Multijugador

## Descripción del Proyecto
Este proyecto consiste en implementar un juego del ahorcado (Hangman) en modo multijugador. El servidor permite la conexión de al menos dos jugadores que se turnan en cada ronda: uno adivina la palabra y el otro la proporciona. Los roles se intercambian en cada ronda, permitiendo la interacción continua entre los jugadores.

## Objetivos
- Desarrollar un juego interactivo de Hangman en el que dos jugadores puedan competir.
- Implementar una comunicación cliente-servidor para gestionar las rondas y el intercambio de información.
- Permitir la conexión simultánea de múltiples jugadores, con roles cambiantes en cada ronda.

## Lenguajes de Programación y Tecnologías
- **Servidor:**  
  - Lenguaje: (Por definir, se contempla usar Java o C)
  - Sistema operativo: Linux (ejecutándose en contenedores Docker o en un servidor Linux)
- **Cliente:**  
  - Lenguaje: C
  - Sistema operativo: Windows (compilado con VS Code/MinGW o similar)

## Estructura del Proyecto
El repositorio contiene las siguientes carpetas:

- **/cliente:**  
  Contiene el código fuente del cliente en C.
- **/servidor:**  
  Contiene el código fuente del servidor (proporcionado por el profesor y adaptado, o implementado en Java/C según se defina).
- **/docs:**  
  Documentación y entregables (incluye este PDF de evidencias).

## Cómo Ejecutar el Proyecto
1. **Servidor:**  
   - Clonar el repositorio y compilar el código del servidor (en Linux).  
   - Ejecutar el servidor en el puerto 5000 (o el puerto definido).
2. **Cliente:**  
   - En Windows, compilar el código del cliente en C.
   - Ejecutar el cliente usando:
     ```
     cliente.exe <archivo.txt> <ip_del_servidor> 5000
     ```
   - Ejemplo:  
     ```
     cliente.exe palabras.txt 192.168.1.100 5000
     ```

## Requisitos Adicionales
- El cliente lee el nombre del archivo y su contenido, enviándolo al servidor.
- El servidor recibe el nombre y el contenido, y crea un archivo local con el mismo nombre y contenido.
- La comunicación se realiza mediante sockets TCP (o UDP, según lo requiera la actividad).

## Colaboradores
- Nombre 1 (GitHub: @nickname1)
- Nombre 2 (GitHub: @nickname2)
- Nombre 3 (GitHub: @nickname3)

