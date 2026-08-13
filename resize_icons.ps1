Add-Type -AssemblyName System.Drawing

$baseImgPath = "C:\Users\salih\.gemini\antigravity\brain\a6c8972a-08a3-4e9d-be7d-4a44e8e37026\app_logo.png"
$outDir = "d:\Projelerim\optimum\app\src\main\res"

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$baseImg = [System.Drawing.Image]::FromFile($baseImgPath)

function Resize-Image {
    param (
        [System.Drawing.Image]$img,
        [int]$size,
        [string]$outPath
    )
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $size, $size)
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $folderPath = Join-Path $outDir $folder
    if (-not (Test-Path $folderPath)) {
        New-Item -ItemType Directory -Path $folderPath | Out-Null
    }
    Resize-Image -img $baseImg -size $size -outPath (Join-Path $folderPath "ic_launcher.png")
    Resize-Image -img $baseImg -size $size -outPath (Join-Path $folderPath "ic_launcher_round.png")
}

# Adaptive foreground
$xxxhdpiPath = Join-Path $outDir "mipmap-xxxhdpi"
Resize-Image -img $baseImg -size 432 -outPath (Join-Path $xxxhdpiPath "ic_launcher_foreground.png")

$baseImg.Dispose()
Write-Host "Icons generated successfully via PowerShell!"
