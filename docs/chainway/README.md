# Chainway SDK

## RFIDWithUHFUsb (R3)

1. App `jna-5.4.0.jar` and `jna-platform-5.4.0.jar`
   The App project needs to import `jna-5.4.0.jar` and `jna-platform-5.4.0.jar`
   windows:
2. `UHFAPI.dll` and `setDllOrSOFilePath.dll`
2. Need to depend on, `UHFAPI.dll`, save these two files to the program root directory. You can also set the dll path to load by calling the setDllOrSOFilePath function.
   Linux:
2. `libTagReader.so`, `setDllOrSOFilePath` -> `so`
2. Need to depend on, libTagReader.so, save these two files to the program root directory. You can also set the so path to load by calling the setDllOrSOFilePath function.