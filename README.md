# Hangman Multijugador

## Descripción del Proyecto

Este proyecto consiste en implementar un juego de Ahorcado (Hangman) en modo multijugador. El sistema está diseñado para que dos jugadores se conecten a través de una arquitectura cliente-servidor. En cada ronda, un jugador ingresa la palabra secreta y el otro intenta adivinarla. Los roles se intercambian en cada ronda.

## Objetivos

- Permitir la conexión de al menos dos jugadores a través de sockets.
- Gestionar la interacción por rondas: 
  - **Cliente 1:** Ingresará la palabra secreta, por ejemplo, "Aguacate".
  - **Cliente 2:** Recibirá la palabra oculta (por ejemplo, "________") y podrá intentar adivinar letras o la palabra completa.
  - A medida que se van adivinando letras, se actualiza en tiempo real el progreso de la palabra oculta.
- Implementar el intercambio de mensajes en formato JSON (o bien, cada mensaje en una línea) para que ambos clientes actualicen la partida.

## Lenguajes de Programación y Tecnologías

- **Servidor:**  
  - Lenguaje: C (usando el código proporcionado por el profesor)  
  - Sistema Operativo: Linux  
  - Ejecución: En contenedores Docker (o en una máquina Linux)

- **Cliente:**  
  - Lenguaje: C  
  - Sistema Operativo: Windows  
  - Ejecución: Con Visual Studio Code (o similar) usando MinGW o compilado con Visual Studio (si se ajusta a las indicaciones del profesor)

## Requisitos Adicionales

- **Comunicación:**  
  El cliente se ejecuta con:
  <cliente ejecutable> <ip> <puerto>

Donde el puerto se fija en 5000. Al conectarse, el sistema solicitará a uno de los clientes que ingrese la palabra a proporcionar (por ejemplo, "Aguacate"). Esa palabra se transformará en un mensaje (por ejemplo, en formato JSON o una cadena con un separador) y se enviará para que el otro cliente la reciba de forma oculta (mostrándose con guiones bajos, como "________").  
Luego, ambos clientes verán el progreso en cada intento, por ejemplo:  
- Cliente 1 – Palabra a dar: "Aguacate"  
- Cliente 2 – Palabra a adivinar: "________"  
- Cliente 2 ingresa: "a"  
- Ambos ven: "A__a_a__"  
y así sucesivamente.

- **Evidencia de la Comunicación:**  
La comunicación se realiza mediante sockets TCP (en el servidor se usa el código del profesor, que ya soporta interacción continua) y el cliente implementa la parte interactiva. Los mensajes se pueden estructurar en JSON o en un formato de texto simple con delimitadores (por ejemplo, cada mensaje en una línea), siempre que ambos extremos interpreten correctamente los datos.

## Estructura del Proyecto

El repositorio se organiza de la siguiente manera:

- `/cliente`:  
Contiene el código fuente del cliente en C (ej. `tcpcliente.c` o `udpcliente.c` según el protocolo).

- `/servidor`:  
Contiene el código fuente del servidor en C (ej. `tcpserver.c` o `udpserver.c` según el protocolo).

- `/docs`:  
Contiene la documentación adicional, incluyendo este README, el PDF de entrega y capturas de pantalla.

## Cómo Ejecutar el Proyecto

1. **Servidor:**  
 - En Linux (por ejemplo, en Docker), compila y ejecuta el servidor usando el código proporcionado por el profesor.  
 - Ejemplo de ejecución:  
   ```bash
   ./tcpserver 5000
   ```
   (El servidor escucha en el puerto 5000).

2. **Cliente:**  
 - En Windows, compila el cliente (en C) usando Visual Studio Code/MinGW o Visual Studio.  
 - Ejemplo de ejecución:  
   ```bash
   cliente.exe 172.18.2.2 5000
   ```
   O, si se usa la IP del host (por ejemplo, 192.168.1.X), se ingresa esa IP.  
 - Una vez conectado, el cliente solicitará ingresar la palabra secreta y continuará con la interacción del juego.

## Colaboradores

- **Demián Velasco Gómez Llanos** (GitHub: @DemianVGLl2)  
- **Nombre 2** (GitHub: @nickname2)  
- **Nombre 3** (GitHub: @nickname3)
