# Hangman Multijugador

## Descripción del Proyecto

Este proyecto consiste en implementar un juego de Ahorcado (Hangman) en modo multijugador. El sistema está diseñado para que dos jugadores se conecten a través de una arquitectura cliente-servidor. En cada ronda, un jugador ingresa la palabra secreta y el otro intenta adivinarla; El primer jugador en unirse al server será el jugador otorgando palabras, mientras que el segundo será el que intente adivinarlas.

## Objetivos

- Permitir la conexión simultánea de dos jugadores mediante sockets.
- Gestionar la interacción:
  - **Cliente 1:** Ingresa la palabra secreta, por ejemplo, "Aguacate".
  - **Cliente 2:** Recibe la palabra oculta (por ejemplo, "_ _ _ _ _ _ _ _") y puede intentar adivinar letras o la palabra completa.
  - A medida que se van adivinando letras, se actualiza en tiempo real el progreso de la palabra oculta (por ejemplo, "A__a_a__").
- Implementar el intercambio de mensajes mediante el uso de reglas de escritura personalizadas a la acción, para que ambos extremos interpreten y actualicen el estado del juego. 

## Lenguajes de Programación y Tecnologías

- **Servidor:**  
  - Lenguaje: C (usando el código proporcionado por el profesor, adaptado para el juego)  
  - Sistema Operativo: Linux  
  - Ejecución: En contenedores Docker (o en una máquina Linux utilizando WSL o Docker)

- **Cliente:**  
  - Lenguaje: Java 
  - Sistema Operativo: Windows  
  - Tecnologías: Con el uso de librerias net de javax para el envío y recepción de mensajes del servidor y JOptionPane, JFrame y JLabel para la interfaz visual del jugador. 

## Requisitos Adicionales

- **Comunicación:**  
  El cliente se ejecuta usando una IDE, o usando la terminal en windows, en la carpeta hangmanClientJava/src/ en donde se instalo el proyecto, ejecutando lo siguiente:
  java Main
donde el puerto se fija en 5000 y el host en 127.0.0.1. Al conectarse, la interfaz solicitará a uno de los clientes que ingrese la palabra secreta (por ejemplo, "Aguacate"). Esa palabra se transformará en un mensaje (Siguiendo las reglas estipuladas en el codigo, ejemplo: el servidor recibirá 3.aguacate) y se enviará para que el otro cliente la reciba de forma oculta (mostrándose como guiones bajos, por ejemplo, "_ _ _ _ _ _ _ _").  
A partir de ahí, ambos clientes verán en tiempo real el progreso del juego.  
Ejemplo de flujo:
- Cliente 1 – Progreso del juego: _ _ _ _ _ _ _ _ 
- Cliente 2 – Palabra a adivinar: "_ _ _ _ _ _ _ _"  
- Cliente 2 ingresa: "a"  
- Ambos ven: "A__a_a__"  
y así sucesivamente.

- **Evidencia de la Comunicación:**  
La comunicación se realiza mediante sockets TCP (el servidor utiliza el código proporcionado por el profesor y soporta interacción continua) y el cliente implementa la parte interactiva. Los mensajes se estructuran de manera estipulada por el codigo en el server para que ambos extremos interpreten correctamente los datos.

## Estructura del Proyecto

El repositorio se organiza de la siguiente manera:

- **/hangmanClientJava:**  
Contiene el código fuente del cliente en Java, que incluye la interfaz gráfica y la lógica de comunicación.

- **/server:**  
Contiene el código fuente del servidor en C (basado en el código proporcionado por el profesor, adaptado para el juego).

- **/clientHangman:**  
Contiene los archivos html, css y javaScript de la versión anterior a Java.

- **/.idea**, **/out:**
Archivos temporales para la IDE de IntelliJIdea

## Cómo Ejecutar el Proyecto

1. **Servidor:**  
    ```bash
   ./tcpserver
   ```

2. **Cliente:**  
 - El cliente se ejecuta usando una IDE, o usando la terminal en windows, en la carpeta hangmanClientJava/src/ en donde se instalo el proyecto, ejecutando lo siguiente:
  java Main.java
 - Se verá de la siguiente manera:
   ```bash
   java Main
   ```
   Si no se pudiera empezar, se puede usar java compiler para recompilar el codigo y despues volverlo a intentar.
   ```bash
   javac Main.java
   java Main
   ```
 - La interfaz gráfica permitirá:
   - Ingresar la palabra secreta (Cliente 1).
   - Visualizar la palabra oculta y adivinar letras o la palabra completa (Cliente 2).

## Colaboradores

- **Demián Velasco Gómez Llanos** (GitHub: @DemianVGLl2)  
- **Santiago Arreola Munguía** (GitHub: @sarreolam)  
- **Marco Antonio Manjarrez Fernández** (GitHub: @MarcoManjarrez)
