# Hangman Multijugador

## Descripción del Proyecto

Este proyecto consiste en implementar un juego de Ahorcado (Hangman) en modo multijugador. El sistema está diseñado para que dos jugadores se conecten a través de una arquitectura cliente-servidor. En cada ronda, un jugador ingresa la palabra secreta y el otro intenta adivinarla; los roles se intercambian en cada ronda, permitiendo una interacción dinámica durante la partida.

## Objetivos

- Permitir la conexión simultánea de al menos dos jugadores mediante sockets.
- Gestionar la interacción por rondas:
  - **Cliente 1:** Ingresa la palabra secreta, por ejemplo, "Aguacate".
  - **Cliente 2:** Recibe la palabra oculta (por ejemplo, "________") y puede intentar adivinar letras o la palabra completa.
  - A medida que se van adivinando letras, se actualiza en tiempo real el progreso de la palabra oculta (por ejemplo, "A__a_a__").
- Implementar el intercambio de mensajes en formato JSON (o en un formato de texto simple con delimitadores) para que ambos extremos interpreten y actualicen el estado del juego.

## Lenguajes de Programación y Tecnologías

- **Servidor:**  
  - Lenguaje: C (usando el código proporcionado por el profesor como base adaptádondolo para el juego)  
  - Sistema Operativo: Linux  
  - Ejecución: En contenedores Docker (o en una máquina Linux, utilizando WSL o Docker)

- **Cliente:**  
  - Lenguaje: Java (con interfaz gráfica)  
  - Sistema Operativo: Windows  
  - Tecnologías: Java (por ejemplo, utilizando JavaFX o Swing para la interfaz gráfica)

## Requisitos Adicionales

- **Comunicación:**  
  El cliente se ejecuta con:
  <cliente ejecutable> <ip> <puerto>
donde el puerto se fija en 5000. Al conectarse, la interfaz gráfica solicitará a uno de los clientes que ingrese la palabra secreta (por ejemplo, "Aguacate"). Esa palabra se transformará en un mensaje (por ejemplo, en formato JSON o en una cadena con separador) y se enviará para que el otro cliente la reciba de forma oculta (mostrándose como guiones bajos, por ejemplo, "________").  
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
Contiene el código fuente del cliente en Java, que incluye la interfaz gráfica y la lógica de comunicación.

- **/servidor:**  
Contiene el código fuente del servidor en C (basado en el código proporcionado por el profesor, adaptado para el juego).

- **/docs:**  
Contiene la documentación adicional, incluyendo este README, el PDF de entrega y las capturas de pantalla.

## Cómo Ejecutar el Proyecto

1. **Servidor:**  
 - En Linux (por ejemplo, en un contenedor Docker o en WSL), compila y ejecuta el servidor usando el código proporcionado por el profesor:
   ```bash
   ./tcpserver 5000
   ```
   (El servidor escucha en el puerto 5000).

2. **Cliente:**  
 - En Windows, compila el cliente en Java (por ejemplo, generando un archivo JAR) con tu IDE preferido.
 - Ejecuta el cliente pasando la IP del servidor (la IP del host a la que se ha mapeado el contenedor) y el puerto 5000:
   ```bash
   java -jar cliente.jar <ip_del_servidor> 5000
   ```
   Ejemplo:
   ```bash
   java -jar cliente.jar 192.168.1.100 5000
   ```
   La interfaz gráfica te permitirá:
   - Ingresar la palabra secreta (Cliente 1).
   - Visualizar la palabra oculta y adivinar letras o la palabra completa (Cliente 2).

## Colaboradores

- **Demián Velasco Gómez Llanos** (GitHub: @DemianVGLl2)  
- **Santiago Arreola Munguia** (GitHub: @sarreolam)  
- **Marco Antonio Manjarrez Fernandez** (GitHub: @MarcoManjarrez)
