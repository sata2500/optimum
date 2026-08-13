Add-Type -AssemblyName System.Drawing

$outDir = "d:\Projelerim\optimum\app\src\main\res"

$sizes = @{
    "drawable-mdpi" = 24
    "drawable-hdpi" = 36
    "drawable-xhdpi" = 48
    "drawable-xxhdpi" = 72
    "drawable-xxxhdpi" = 96
}

function Generate-Notification-Icon {
    param (
        [int]$size,
        [string]$outPath
    )
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $g.Clear([System.Drawing.Color]::Transparent)

    $penWidth = [Math]::Max(2, [int]($size * 0.15))
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, $penWidth)
    
    $padding = [int]($penWidth / 2) + 2
    $w = $size - ($padding * 2)
    $h = $size - ($padding * 2)
    $rect = New-Object System.Drawing.Rectangle($padding, $padding, $w, $h)
    
    $g.DrawEllipse($pen, $rect)

    $center = [int]($size / 2)
    $handWidth = [Math]::Max(1, [int]($penWidth * 0.7))
    $handPen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, $handWidth)
    $handPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $handPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    
    # Minute hand
    $g.DrawLine($handPen, $center, $center, $center, [int]($padding + $penWidth))
    # Hour hand
    $g.DrawLine($handPen, $center, $center, [int]($size - $padding - $penWidth), $center)

    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    $pen.Dispose()
    $handPen.Dispose()
}

foreach ($folder in $sizes.Keys) {
    $size = [int]$sizes[$folder]
    $folderPath = Join-Path $outDir $folder
    if (-not (Test-Path $folderPath)) {
        New-Item -ItemType Directory -Path $folderPath | Out-Null
    }
    Generate-Notification-Icon -size $size -outPath (Join-Path $folderPath "ic_notification.png")
}

Write-Host "Transparent PNG Notification Icons generated successfully!"
