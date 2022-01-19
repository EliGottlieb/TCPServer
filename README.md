README - Eli Gottlieb - Persistent and Non-Persistent Server



This server was tested with Google Chrome Version 95.0.4638.69 (Official Build) (64-bit)
****
Config Notes:

Port and directory need to be changed to use config and html files
Persistency line should be set to "Persistency--true" or "Persistency--false". Changing the true and false strings themselves will also work.

Idle Time will be read and implemented by the server as the timeout value for the socket. Note that this value will be interpreted in ms.
****
Notes:

-Html files have a "link rel="icon" href="data:;base64,="" line in order to stop any GET /favicon.ico HTTP/1.1 requests 

