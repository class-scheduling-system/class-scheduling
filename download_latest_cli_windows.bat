@echo off
REM --------------------------------------------------------------------------------
REM Copyright (c) 2022-NOW(至今) 锋楪技术团队
REM Author: 锋楪技术团队 (https://www.frontleaves.com)
REM
REM 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
REM --------------------------------------------------------------------------------
REM 许可证声明：
REM
REM 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
REM
REM 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
REM 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
REM 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
REM 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
REM
REM 使用本软件即表示您了解此声明并同意其条款。
REM
REM 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
REM https://opensource.org/licenses/MIT
REM --------------------------------------------------------------------------------
REM 免责声明：
REM
REM 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
REM 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
REM --------------------------------------------------------------------------------

:: 获取最新的 Release tag
curl -s https://api.github.com/repos/class-scheduling-system/table-install-cli/releases/latest > temp.json
for /f "tokens=4 delims=:" %%i in ('findstr "tag_name" temp.json') do set "tag=%%~i"
set tag=%tag:~1,-2%
del temp.json

:: 下载最新的 CLI 文件
curl -L -o cli-windows-amd64.exe https://github.com/class-scheduling-system/table-install-cli/releases/download/%tag%/cli-windows-amd64.exe

:: 创建目标目录
if not exist .\db-cli mkdir db-cli

:: 删除旧文件并移动新文件
if exist .\db-cli\cli-windows-amd64.exe del /f .\db-cli\cli-windows-amd64.exe
move /y cli-windows-amd64.exe .\db-cli\cli-windows-amd64.exe

echo CLI downloaded and moved to .\db-cli\cli-windows-amd64.exe
echo Done!
