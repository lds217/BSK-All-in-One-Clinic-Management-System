# Create directories if they don't exist
New-Item -ItemType Directory -Force -Path "BSK-Application"
New-Item -ItemType Directory -Force -Path "BSK-Application\lib"
New-Item -ItemType Directory -Force -Path "BSK-Application\resources"
New-Item -ItemType Directory -Force -Path "BSK-Application\resources\images"
New-Item -ItemType Directory -Force -Path "BSK-Application\resources\icons"
New-Item -ItemType Directory -Force -Path "BSK-Application\resources\fonts"
New-Item -ItemType Directory -Force -Path "BSK-Application\resources\database"
New-Item -ItemType Directory -Force -Path "BSK-Application\config"
New-Item -ItemType Directory -Force -Path "BSK-Application\logs"

# Create resources directory in source if it doesn't exist
New-Item -ItemType Directory -Force -Path "src\main\resources\images"
New-Item -ItemType Directory -Force -Path "src\main\resources\icons"
New-Item -ItemType Directory -Force -Path "src\main\resources\fonts"

# Copy assets from the UI assets folder
if (Test-Path "src\main\java\BsK\client\ui\assets\img") {
    Copy-Item "src\main\java\BsK\client\ui\assets\img\*" -Destination "src\main\resources\images\" -Force -Recurse
    Copy-Item "src\main\java\BsK\client\ui\assets\img\*" -Destination "BSK-Application\resources\images\" -Force -Recurse
}

if (Test-Path "src\main\java\BsK\client\ui\assets\icon") {
    Copy-Item "src\main\java\BsK\client\ui\assets\icon\*" -Destination "src\main\resources\icons\" -Force -Recurse
    Copy-Item "src\main\java\BsK\client\ui\assets\icon\*" -Destination "BSK-Application\resources\icons\" -Force -Recurse
}

if (Test-Path "src\main\java\BsK\client\ui\assets\font") {
    Copy-Item "src\main\java\BsK\client\ui\assets\font\*" -Destination "src\main\resources\fonts\" -Force -Recurse
    Copy-Item "src\main\java\BsK\client\ui\assets\font\*" -Destination "BSK-Application\resources\fonts\" -Force -Recurse
}

# Copy executables
Copy-Item "target\BSK-Server.exe" -Destination "BSK-Application\" -Force
Copy-Item "target\BSK-Client.exe" -Destination "BSK-Application\" -Force

# Copy JAR files
Copy-Item "target\bsk-server-jar-with-dependencies.jar" -Destination "BSK-Application\lib\" -Force
Copy-Item "target\bsk-client-jar-with-dependencies.jar" -Destination "BSK-Application\lib\" -Force

# Copy all resources from src/main/resources
Copy-Item "src\main\resources\*" -Destination "BSK-Application\resources\" -Recurse -Force

# Create log4j2 configuration file
@"
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <RollingFile name="RollingFile" 
                     fileName="logs/bsk.log"
                     filePattern="logs/bsk-%d{yyyy-MM-dd}-%i.log">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <SizeBasedTriggeringPolicy size="10 MB"/>
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
            </Policies>
            <DefaultRolloverStrategy max="10"/>
        </RollingFile>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="RollingFile"/>
        </Root>
    </Loggers>
</Configuration>
"@ | Out-File -FilePath "BSK-Application\config\log4j2.xml" -Encoding UTF8 -Force

# Create config file with detailed comments
@"
# BSK Clinic Management System Configuration
# ----------------------------------------
# This configuration file is used by both the server and client applications.

# Server Configuration
# -------------------
# The address where the server is running
# Use "localhost" if running server and client on the same machine
# Use the actual IP address if running on different machines
# Examples:
#   server.address=localhost        (same machine)
#   server.address=192.168.1.100   (local network)
#   server.address=example.com     (domain name)
server.address=localhost

# The port number that the server will listen on and clients will connect to
# Default: 1999
server.port=1999

# Log Configuration
# ----------------
# Log files will be created in the logs directory
# See config/log4j2.xml for detailed logging configuration

# Important Notes:
# 1. Server Address:
#    - Use "localhost" if server and client are on the same machine
#    - Use the server's IP address if running on different machines
#    - Make sure the server is reachable from the client machine
#
# 2. After changing any settings:
#    - Close all running clients
#    - Stop the server
#    - Start the server again
#    - Start the clients
#
# 3. Network Configuration:
#    - Ensure the chosen port is not blocked by firewalls
#    - If using a domain name, make sure DNS resolution works
#    - For remote connections, the server must be accessible from the client
"@ | Out-File -FilePath "BSK-Application\config\config.properties" -Encoding UTF8 -Force

# Create batch files for easy startup
@"
@echo off
echo Starting BSK Server...
start "" "BSK-Server.exe"
"@ | Out-File -FilePath "BSK-Application\start-server.bat" -Encoding ASCII -Force

@"
@echo off
echo Starting BSK Client...
start "" "BSK-Client.exe"
"@ | Out-File -FilePath "BSK-Application\start-client.bat" -Encoding ASCII -Force

# Create README file
@"
BSK Clinic Management System
===========================

This folder contains both the server and client applications for the BSK Clinic Management System.

Quick Start
----------
1. Run 'start-server.bat' to start the server
2. Run 'start-client.bat' to start the client

Directory Structure
-----------------
BSK-Application/
├── BSK-Server.exe        - Server application
├── BSK-Client.exe        - Client application
├── start-server.bat      - Server startup script
├── start-client.bat      - Client startup script
├── lib/                  - Required libraries
├── resources/            - Application resources
│   ├── images/          - Images
│   ├── icons/           - Icons
│   ├── fonts/           - Fonts
│   └── database/        - Database files
├── config/              - Configuration files
│   ├── config.properties - Main configuration
│   └── log4j2.xml       - Logging configuration
└── logs/                - Application logs

Configuration
------------
1. Edit config/config.properties to change:
   - Server address (default: localhost)
   - Server port (default: 1999)

2. Logging:
   - Logs are stored in the logs/ directory
   - Configure logging in config/log4j2.xml
   - Log files rotate automatically

Important Notes
-------------
1. Always start the server before the client
2. After changing config.properties:
   - Stop all clients
   - Stop the server
   - Start server again
   - Start clients

Troubleshooting
--------------
1. Check logs/ directory for error messages
2. Verify server is running before starting clients
3. Ensure config.properties has correct server address
4. Check if port 1999 is not blocked by firewall

Support
-------
For technical support, please contact:
[Your support contact information]
"@ | Out-File -FilePath "BSK-Application\README.txt" -Encoding UTF8 -Force

Write-Host "`nApplication has been built and organized in the BSK-Application folder!"
Write-Host "`nTo start the application:"
Write-Host "1. Run start-server.bat"
Write-Host "2. Run start-client.bat"
Write-Host "`nConfiguration files are in the config/ directory"
Write-Host "Logs will be written to the logs/ directory" 