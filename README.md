# concurrent-chat
TCP server-client concurrent chat, for local network.

## TCP Server:
Must define the port in use.
Accepts connections from multiple clients.
A default username is attributed to each client.
Received messages are broadcast to all connected clients.

## TCP Client:
Must define the server port.
Allows the use of specific commands:

|COMMAND                 |DESCRIPTION                    |
|------------------------|-------------------------------|
|"/quit"                 |`client exits the chat`                |
|"/list"                 |`client receives a list of all clients`            |
|"/whisper username"     |`sends a private message to a specified user`|
| "/anon"		             |`sends an annonynous message to all users`|
|"/user username"        |`changes the username to the specified one` |
