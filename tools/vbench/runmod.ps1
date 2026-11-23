# -Work: folder for the game dir and screenshots; -Base: folder with real-cp.txt (classpath of Forge 1.7.10)
param([Parameter(Mandatory = $true)][string]$Work, [Parameter(Mandatory = $true)][string]$Base, [switch]$NoOptiFine, [string]$Tag = 'mod')
$ErrorActionPreference = 'Continue'
$u = New-Object Text.UTF8Encoding($false)
$V = $Work
$oldb = $Base
$mc = "$env:APPDATA\.minecraft"
$g = "$V\g$Tag"
$shots = "$V\shots$Tag"
Remove-Item -Recurse -Force "$g\saves", "$g\screenshots", $shots -Confirm:$false -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$g\mods", $shots | Out-Null
Get-ChildItem "$g\mods" -File | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -Confirm:$false }
Copy-Item (Get-ChildItem "$PSScriptRoot\..\..\build\release\mc112bridge-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName "$g\mods\"
eleasemc112bridge-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName "$gmods\"
$opts = @('fov:0.0', 'renderDistance:8', 'guiScale:0', 'fancyGraphics:true', 'ao:2', 'clouds:false', 'particles:0', 'maxFps:60',
    'enableVsync:false', 'mipmapLevels:4', 'lang:en_US', 'pauseOnLostFocus:false', 'gamma:0.0', 'useVbo:true', 'bobView:true',
    'soundCategory_master:0.0', 'chatVisibility:0')
[IO.File]::WriteAllText("$g\options.txt", ($opts -join "`n") + "`n", $u)
$cps = [IO.File]::ReadAllText("$oldb\real-cp.txt", $u).Trim()
if ($NoOptiFine) { $cps = ($cps.Split(';') | Where-Object { $_ -notmatch 'OptiFine-1\.7\.10' }) -join ';' }
$verdir = "$mc\versions\ForgeOptiFine 1.7.10"; $java = "$env:APPDATA\.tlauncher\jvms\jre1.8.0_281\bin\java.exe"
$a = @('-Xmx3000M', "-Djava.library.path=$verdir\natives", '-Dfml.ignoreInvalidMinecraftCertificates=true', '-Dfml.ignorePatchDiscrepancies=true', '-cp', $cps,
    'net.minecraft.launchwrapper.Launch', '--username', 'AuditBot', '--version', '1.7.10', '--gameDir', $g, '--assetsDir', "$mc\assets",
    '--assetIndex', '1.7.10', '--uuid', '00000000000000000000000000000000', '--accessToken', '0', '--userProperties', '{}', '--userType', 'legacy',
    '--tweakClass', 'cpw.mods.fml.common.launcher.FMLTweaker', '--width', '854', '--height', '480')
$env:MC112_AUDIT = 'visual'; $env:MC112_SCENE = "$V\scenes.txt"; Remove-Item env:MC112_WORLD -ErrorAction SilentlyContinue
$p = Start-Process -FilePath $java -ArgumentList ($a | ForEach-Object { if ($_ -match '[ ;{}]') { '"' + $_ + '"' } else { $_ } }) -WorkingDirectory $g -PassThru -NoNewWindow -RedirectStandardOutput "$g\out.log" -RedirectStandardError "$g\err.log"
if (-not $p.WaitForExit(900000)) { "TIMEOUT"; $p.Kill() }
Copy-Item "$g\screenshots\*.png" $shots -ErrorAction SilentlyContinue
(Get-ChildItem $shots -Filter *.png).Count.ToString() + " shots ($Tag)"
Get-Content "$g\audit112-visual.txt" -ErrorAction SilentlyContinue | Select-Object -First 5
