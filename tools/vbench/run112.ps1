# -Work: folder for the game dir and screenshots; -H112: folder with client-unsigned.jar and cp.txt (classpath of 1.12.2)
param([Parameter(Mandatory = $true)][string]$Work, [Parameter(Mandatory = $true)][string]$H112)
$ErrorActionPreference = 'Continue'
$u = New-Object Text.UTF8Encoding($false)
$V = $Work
$H = $H112
$mc = "$env:APPDATA\.minecraft"
$g = "$V\g112"
$shots = "$V\shots112"
Remove-Item -Recurse -Force "$g\saves", $shots -Confirm:$false -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $g, $shots | Out-Null
# the same picture settings as the mod run
$opts = @('version:1343', 'fov:0.0', 'renderDistance:8', 'guiScale:0', 'fancyGraphics:true', 'ao:2', 'renderClouds:false',
    'particles:0', 'maxFps:60', 'enableVsync:false', 'mipmapLevels:4', 'lang:en_us', 'tutorialStep:none', 'showSubtitles:false',
    'pauseOnLostFocus:false', 'gamma:0.0', 'useVbo:true', 'attackIndicator:1', 'entityShadows:true', 'bobView:true',
    'soundCategory_master:0.0', 'chatVisibility:0')
[IO.File]::WriteAllText("$g\options.txt", ($opts -join "`n") + "`n", $u)
$cp = ([IO.File]::ReadAllText("$H\cp.txt", $u).Trim()).Replace("$mc\versions\1.12.2\1.12.2.jar", "$H\client-unsigned.jar") + ";$V\c"
$java = "$env:APPDATA\.tlauncher\jvms\jre1.8.0_281\bin\java.exe"
$a = @('-Xmx2500M', "-Djava.library.path=$mc\versions\1.12.2\natives", '-cp', $cp, 'HarnessScene', '--username', 'AuditBot', '--version', '1.12.2',
    '--gameDir', $g, '--assetsDir', "$mc\assets", '--assetIndex', '1.12', '--uuid', '00000000000000000000000000000000', '--accessToken', '0',
    '--userType', 'legacy', '--width', '854', '--height', '480', "$V\scenes.txt", $shots)
$p = Start-Process -FilePath $java -ArgumentList ($a | ForEach-Object { if ($_ -match '[ ;{}]') { '"' + $_ + '"' } else { $_ } }) -WorkingDirectory $g -PassThru -NoNewWindow -RedirectStandardOutput "$g\out.log" -RedirectStandardError "$g\err.log"
if (-not $p.WaitForExit(600000)) { "TIMEOUT"; $p.Kill() }
(Get-ChildItem $shots -Filter *.png).Count.ToString() + " shots"
Select-String -Path "$g\out.log" -Pattern "Exception|Error" | Select-Object -First 5 | ForEach-Object { $_.Line }
