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
  El cliente se ejecuta usando una IDE, o usando la terminal en windows, en la carpeta en la que se instalo el proyecto, ejecutando lo siguiente:
  java 
donde el puerto se fija en 5000. Al conectarse, la interfaz solicitará a uno de los clientes que ingrese la palabra secreta (por ejemplo, "Aguacate"). Esa palabra se transformará en un mensaje (por ejemplo, en formato JSON o una cadena con delimitador) y se enviará para que el otro cliente la reciba de forma oculta (mostrándose como guiones bajos, por ejemplo, "________").  
A partir de ahí, ambos clientes verán en tiempo real el progreso del juego.  
Ejemplo de flujo:
- Cliente 1 – Palabra a dar: "Aguacate"  
- Cliente 2 – Palabra a adivinar: "________"  
- Cliente 2 ingresa: "a"  
- Ambos ven: "A__a_a__"  
y así sucesivamente.

- **Evidencia de la Comunicación:**  
La comunicación se realiza mediante sockets TCP (el servidor utiliza el código proporcionado por el profesor y soporta interacción continua) y el cliente implementa la parte interactiva. Los mensajes se estructuran en formato JSON (o en un formato de texto simple con delimitadores) para que ambos extremos interpreten correctamente los datos.

## Estructura del Proyecto

El repositorio se organiza de la siguiente manera:

- **/cliente:**  
Contiene el código fuente del cliente en JavaScript, que incluye la interfaz gráfica y la lógica de comunicación (por ejemplo, usando WebSockets o TCP mediante Node.js).

- **/servidor:**  
Contiene el código fuente del servidor en C (basado en el código proporcionado por el profesor, adaptado para el juego).

- **/docs:**  
Contiene la documentación adicional, incluyendo este README, el PDF de entrega y las capturas de pantalla.

## Cómo Ejecutar el Proyecto

1. **Servidor:**  
 - En Linux (por ejemplo, en un contenedor Docker o en WSL), compila y ejecuta el servidor usando el código proporcionado:
   ```bash
   ./tcpserver 5000
   ```
   (El servidor escucha en el puerto 5000).

2. **Cliente:**  
 - En Windows, implementa el cliente en JavaScript (por ejemplo, como una aplicación web o mediante Node.js).
 - Si se trata de una aplicación web, abre la página en tu navegador y se solicitarán los parámetros (o se configurarán internamente) para conectarse al servidor, usando la IP del servidor y el puerto 5000.
 - Alternativamente, si usas Node.js, ejecuta:
   ```bash
   node cliente.js <ip_del_servidor> 5000
   ```
 - La interfaz gráfica permitirá:
   - Ingresar la palabra secreta (Cliente 1).
   - Visualizar la palabra oculta y adivinar letras o la palabra completa (Cliente 2).

## Colaboradores

- **Demián Velasco Gómez Llanos** (GitHub: @DemianVGLl2)  
- **Santiago Arreola Munguía** (GitHub: @sarreolam)  
- **Marco Antonio Manjarrez Fernández** (GitHub: @MarcoManjarrez)
