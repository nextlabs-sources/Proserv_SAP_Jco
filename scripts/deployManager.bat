@echo off
REM ############################################################
REM ## SAP JCo Install Script
REM ## OS: Windows; PC: CE-Policy Controller
REM ############################################################
SETLOCAL ENABLEDELAYEDEXPANSION

REM #### Identify 64/32bit ###
IF "%~1"=="" (SET Computer=%ComputerName%) ELSE (SET Computer=%~1)
IF /I NOT "%Computer%"=="%ComputerName%" (
	PING %Computer% -n 2 2>NUL | FIND "TTL=" >NUL
)
FOR /F "tokens=2 delims==" %%A IN ('WMIC /Node:"%Computer%" Path Win32_Processor Get AddressWidth /Format:list') DO SET OSB=%%A

REM #### Default values
SET NXL_PC_HOME=C:\Program Files\NextLabs\Policy Controller\
SET CLIENT_HOST=sap-dev03.demo20.nextlabs.com
SET CLIENT_SYSNR=00
SET CLIENT_ID=100
SET CLIENT_USER=nxl_comm
SET CLIENT_PASSWD=nextlabs123
SET CLIENT_PASSWD_ENC=sab8034feaa025a64b4b33a473f519a1f
SET GATEWAY_HOST=sap-dev03.demo20.nextlabs.com
SET GATEWAY_SERV=sapgw00
SET GATEWAY_PRGID=NXL_CONNECT_TO_PC

:ShowMainOptions
cls
echo ######### SAP JCo Deployment Manager ###########
echo.
echo NOTE: Please STOP Policy Controller before proceeding further.
echo.
echo    [1] Set root directory of Policy Controller [!NXL_PC_HOME!]. 
echo    [8] Install
echo    [9] Uninstall
echo    [0] Exit
echo.
GOTO ChooseMainOption

:ChooseMainOption
set CO=0
set /P CO=Choose an option [default is !CO!] :
IF  %CO% NEQ 1 IF %CO% NEQ 8 IF %CO% NEQ 9 IF %CO% NEQ 0 GOTO ShowMainOptions
set MO=MainOption!CO!
GOTO %MO%

:MainOption1
set /P NXL_PC_HOME=Enter root directory of PC[default is !NXL_PC_HOME!] :
set NXL_PC_HOME=!NXL_PC_HOME:"=!
GOTO ShowMainOptions

:MainOption8
cls
echo ######### SAP JCo Deployment Manager ###########
echo.
echo Installation Menu
echo.
echo    [1] Set Client Host [!CLIENT_HOST!]
echo    [2] Set Client System No. [!CLIENT_SYSNR!] 
echo    [3] Set Client ID [!CLIENT_ID!]
echo    [4] Set Client User [!CLIENT_USER!]
echo    [5] Set Client Password [!CLIENT_PASSWD!]
echo    [6] Set Gateway Host [!GATEWAY_HOST!]
echo    [7] Set Gateway Service [!GATEWAY_SERV!]
echo    [8] Set Gateway Program ID [!GATEWAY_PRGID!]
echo    [9] Proceed with INSTALLATION. Make sure all above values are correctly set.
echo    [0] Back
echo.
GOTO ChooseInsOption

:MainOption9
cls
echo ######### SAP JCo Deployment Manager ###########
echo.
echo Uninstallation Menu
echo.
echo    [9] Proceed with UNINSTALLATION. All SAP JCo binaries and SAPJavaSDKService.properties will be deleted.
echo    [0] Back
echo.
GOTO ChooseUninsOption

:MainOption0
GOTO FinalExit

:FinalExit
set /p DUMMY=Hit ENTER to complete.
EXIT /B

:ChooseInsOption
set CO=0
set /P CO=Choose an option [default is !CO!] :
IF  %CO% NEQ 0 IF %CO% NEQ 1 IF %CO% NEQ 2 IF %CO% NEQ 3 IF %CO% NEQ 4 IF %CO% NEQ 5 IF %CO% NEQ 6 IF %CO% NEQ 7 IF %CO% NEQ 8 IF %CO% NEQ 9 GOTO MainOption8
set IO=InsOption!CO!
GOTO %IO%

:InsOption0
GOTO ShowMainOptions

:InsOption1
set /P CLIENT_HOST=Enter Client Host[default is !CLIENT_HOST!] :
GOTO MainOption8

:InsOption2
set /P CLIENT_SYSNR=Enter Client System No.[default is !CLIENT_SYSNR!] :
GOTO MainOption8

:InsOption3
set /P CLIENT_ID=Enter Client ID[default is !CLIENT_ID!] :
GOTO MainOption8

:InsOption4
set /P CLIENT_USER=Enter Client User[default is !CLIENT_USER!] :
GOTO MainOption8

:InsOption5
set /P CLIENT_PASSWD=Enter Client Password[default is !CLIENT_PASSWD!] :
set CURR_DIR=%~dp0
CD %NXL_PC_HOME%jre\bin
for /f "usebackq delims=" %%a in (`java.exe -classpath "../../jlib/crypt.jar;../../jlib/common-framework.jar" com.bluejungle.framework.crypt.Encryptor -password !CLIENT_PASSWD!`) DO SET CLIENT_PASSWD_ENC=%%a
CD %CURR_DIR%
GOTO MainOption8

:InsOption6
set /P GATEWAY_HOST=Enter Gateway Host[default is !GATEWAY_HOST!] :
GOTO MainOption8

:InsOption7
set /P GATEWAY_SERV=Enter Gateway Service[default is !GATEWAY_SERV!] :
GOTO MainOption8

:InsOption8
set /P GATEWAY_PRGID=Enter Gateway Program ID[default is !GATEWAY_PRGID!] :
GOTO MainOption8

:InsOption9
echo Installing SAP JCo ...
echo #Auto Generated>.\config\temp.properties
FOR /F "tokens=* delims=" %%x in (.\config\SAPJavaSDKService.properties) DO (
	set FILE_CONTENT=%%x
	set FILE_CONTENT=!FILE_CONTENT:$CLIENT_HOST$=%CLIENT_HOST%!
	set FILE_CONTENT=!FILE_CONTENT:$CLIENT_SYSNR$=%CLIENT_SYSNR%!
	set FILE_CONTENT=!FILE_CONTENT:$CLIENT_ID$=%CLIENT_ID%!
	set FILE_CONTENT=!FILE_CONTENT:$CLIENT_USER$=%CLIENT_USER%!
	set FILE_CONTENT=!FILE_CONTENT:$CLIENT_PASSWD$=%CLIENT_PASSWD_ENC%!
	set FILE_CONTENT=!FILE_CONTENT:$GATEWAY_HOST$=%GATEWAY_HOST%!
	set FILE_CONTENT=!FILE_CONTENT:$GATEWAY_SERV$=%GATEWAY_SERV%!
	set FILE_CONTENT=!FILE_CONTENT:$GATEWAY_PRGID$=%GATEWAY_PRGID%!
	echo !FILE_CONTENT!>>.\config\temp.properties
)
REM #### Create necessary folders ###
IF NOT EXIST "%NXL_PC_HOME%\jservice\jar\sap\" MKDIR "%NXL_PC_HOME%\jservice\jar\sap\"
IF NOT EXIST "%NXL_PC_HOME%\jservice\config\" MKDIR "%NXL_PC_HOME%\jservice\config\"
IF NOT EXIST "%NXL_PC_HOME%\jre\lib\ext\" MKDIR "%NXL_PC_HOME%\jre\lib\ext\"
REM #### Copy files ###
COPY .\config\temp.properties "%NXL_PC_HOME%\jservice\config\SAPJavaSDKService.properties" >NUL
DEL  .\config\temp.properties >NUL
COPY .\jars\SAPJCo-*.jar "%NXL_PC_HOME%\jservice\jar\sap\" >NUL
IF "%OSB%"=="64" (
	COPY .\xlib\NTamd64\*.jar "%NXL_PC_HOME%\jre\lib\ext\" >NUL
	COPY .\xlib\NTamd64\*.dll "%NXL_PC_HOME%\jre\lib\ext\" >NUL
)
IF "%OSB%"=="32" (
	COPY .\xlib\NTia32\*.jar "%NXL_PC_HOME%\jre\lib\ext\" >NUL
	COPY .\xlib\NTia32\*.dll "%NXL_PC_HOME%\jre\lib\ext\" >NUL
)
echo DONE.
GOTO FinalExit

:ChooseUninsOption
set CO=0
set /P CO=Choose an option [default is !CO!] :
IF  %CO% NEQ 0 IF %CO% NEQ 9 GOTO MainOption9
set UO=UninsOption!CO!
GOTO %UO%

:UninsOption0
GOTO ShowMainOptions

:UninsOption9
echo Uninstalling SAP JCo ...
REM #### Delete files ###
DEL /Q "%NXL_PC_HOME%\jservice\config\SAPJavaSDKService.properties" 2>NUL
DEL /Q "%NXL_PC_HOME%\jservice\jar\sap\*.*" 2>NUL
DEL /Q "%NXL_PC_HOME%\jre\lib\ext\sapjco3.*" 2>NUL
echo DONE.
GOTO FinalExit