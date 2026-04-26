Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$OutDir = Join-Path $Root 'promo_images'
$OutPath = Join-Path $OutDir 'twelve_talismans_promo.png'
$TextureDir = Join-Path $Root 'src\main\resources\assets\chen_mod\textures\item\talisman'

if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
}

$Talismans = @(
    @{ Label = 'dragon';  File = 'dragon_talisman.png';  Color = [System.Drawing.Color]::FromArgb(255, 232, 66, 40) },
    @{ Label = 'horse';   File = 'horse_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 92, 202, 255) },
    @{ Label = 'monkey';  File = 'monkey_talisman.png';  Color = [System.Drawing.Color]::FromArgb(255, 184, 112, 255) },
    @{ Label = 'mouse';   File = 'mouse_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 116, 226, 255) },
    @{ Label = 'ox';      File = 'ox_talisman.png';      Color = [System.Drawing.Color]::FromArgb(255, 255, 216, 82) },
    @{ Label = 'pig';     File = 'pig_talisman.png';     Color = [System.Drawing.Color]::FromArgb(255, 255, 126, 174) },
    @{ Label = 'rabbit';  File = 'rabbit_talisman.png';  Color = [System.Drawing.Color]::FromArgb(255, 114, 255, 126) },
    @{ Label = 'rooster'; File = 'rooster_talisman.png'; Color = [System.Drawing.Color]::FromArgb(255, 255, 166, 62) },
    @{ Label = 'sheep';   File = 'sheep_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 154, 188, 255) },
    @{ Label = 'snake';   File = 'snack_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 70, 238, 160) },
    @{ Label = 'tiger';   File = 'tiger_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 255, 244, 118) },
    @{ Label = 'dog';     File = 'dog_talisman.png';     Color = [System.Drawing.Color]::FromArgb(255, 236, 224, 156) }
)

function New-Font {
    param(
        [string] $Family,
        [float] $Size,
        [System.Drawing.FontStyle] $Style = [System.Drawing.FontStyle]::Regular
    )

    try {
        return New-Object System.Drawing.Font($Family, $Size, $Style, [System.Drawing.GraphicsUnit]::Pixel)
    } catch {
        return New-Object System.Drawing.Font('Arial', $Size, $Style, [System.Drawing.GraphicsUnit]::Pixel)
    }
}

function New-GlowBitmap {
    param(
        [int] $Width,
        [int] $Height,
        [System.Drawing.Color] $Color,
        [int] $Strength = 48
    )

    $bmp = New-Object System.Drawing.Bitmap $Width, $Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    for ($i = 0; $i -lt 24; $i++) {
        $ratio = $i / 24.0
        $padX = [int]($ratio * $Width / 2)
        $padY = [int]($ratio * $Height / 2)
        $alpha = [int]($Strength * [Math]::Pow(1.0 - $ratio, 2.45))
        $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($alpha, $Color))
        $g.FillEllipse($brush, $padX, $padY, $Width - 2 * $padX, $Height - 2 * $padY)
        $brush.Dispose()
    }

    $g.Dispose()
    return $bmp
}

function Draw-CenteredText {
    param(
        [System.Drawing.Graphics] $Graphics,
        [string] $Text,
        [System.Drawing.Font] $Font,
        [System.Drawing.Brush] $Brush,
        [float] $CenterX,
        [float] $Y
    )

    $size = $Graphics.MeasureString($Text, $Font)
    $Graphics.DrawString($Text, $Font, $Brush, $CenterX - $size.Width / 2, $Y)
}

function Draw-Talisman {
    param(
        [System.Drawing.Graphics] $Graphics,
        [hashtable] $Talisman,
        [float] $X,
        [float] $Y,
        [int] $Size,
        [float] $Angle
    )

    $glow = New-GlowBitmap -Width ([int]($Size * 2.1)) -Height ([int]($Size * 2.1)) -Color $Talisman.Color -Strength 82
    $Graphics.DrawImage($glow, [int]($X - $glow.Width / 2), [int]($Y - $glow.Height / 2), $glow.Width, $glow.Height)
    $glow.Dispose()

    $shadow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(165, 0, 0, 0))
    $Graphics.FillEllipse($shadow, [int]($X - $Size * 0.65), [int]($Y - $Size * 0.54 + 12), [int]($Size * 1.30), [int]($Size * 1.08))
    $shadow.Dispose()

    $back = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(120, 20, 23, 28))
    $Graphics.FillEllipse($back, [int]($X - $Size * 0.64), [int]($Y - $Size * 0.64), [int]($Size * 1.28), [int]($Size * 1.28))
    $back.Dispose()

    $ringOuter = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(210, $Talisman.Color)), 5
    $ringInner = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(110, 255, 247, 218)), 2
    $Graphics.DrawEllipse($ringOuter, [int]($X - $Size * 0.68), [int]($Y - $Size * 0.68), [int]($Size * 1.36), [int]($Size * 1.36))
    $Graphics.DrawEllipse($ringInner, [int]($X - $Size * 0.58), [int]($Y - $Size * 0.58), [int]($Size * 1.16), [int]($Size * 1.16))
    $ringOuter.Dispose()
    $ringInner.Dispose()

    $texturePath = Join-Path $TextureDir $Talisman.File
    $tex = [System.Drawing.Bitmap]::FromFile($texturePath)
    $oldTransform = $Graphics.Transform
    $matrix = New-Object System.Drawing.Drawing2D.Matrix
    $matrix.Translate($X, $Y)
    $matrix.Rotate($Angle)
    $Graphics.Transform = $matrix
    $Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $Graphics.DrawImage($tex, [int](-$Size / 2), [int](-$Size / 2), $Size, $Size)
    $Graphics.Transform = $oldTransform
    $Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $tex.Dispose()
}

$width = 1920
$height = 1080
$canvas = New-Object System.Drawing.Bitmap $width, $height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$rect = New-Object System.Drawing.Rectangle 0, 0, $width, $height
$bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $rect,
    [System.Drawing.Color]::FromArgb(255, 10, 14, 22),
    [System.Drawing.Color]::FromArgb(255, 72, 39, 26),
    32
)
$g.FillRectangle($bgBrush, $rect)
$bgBrush.Dispose()

$rng = New-Object System.Random 1209
for ($i = 0; $i -lt 220; $i++) {
    $x = $rng.Next(0, $width)
    $y = $rng.Next(0, 720)
    $s = $rng.Next(1, 4)
    $a = $rng.Next(32, 115)
    $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, 255, 219, 144))
    $g.FillRectangle($brush, $x, $y, $s, $s)
    $brush.Dispose()
}

$moonGlow = New-GlowBitmap -Width 560 -Height 560 -Color ([System.Drawing.Color]::FromArgb(255, 255, 95, 58)) -Strength 42
$g.DrawImage($moonGlow, 1190, -155, 560, 560)
$moonGlow.Dispose()
$moonBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(150, 188, 47, 35))
$g.FillEllipse($moonBrush, 1342, 15, 245, 245)
$moonBrush.Dispose()

$templeBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(230, 19, 21, 27))
$templeBrush2 = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(210, 31, 30, 35))
for ($i = 0; $i -lt 42; $i++) {
    $bw = $rng.Next(44, 92)
    $bh = $rng.Next(55, 190)
    $x = $i * 48 - 25
    $y = 710 - $bh + $rng.Next(-18, 26)
    $b = if ($i % 2 -eq 0) { $templeBrush } else { $templeBrush2 }
    $g.FillRectangle($b, $x, $y, $bw, $bh)
    if ($i % 5 -eq 0) {
        $g.FillRectangle($b, $x - 22, $y - 18, $bw + 44, 18)
    }
}
$templeBrush.Dispose()
$templeBrush2.Dispose()

$floorBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Rectangle 0, 620, $width, 460),
    [System.Drawing.Color]::FromArgb(215, 37, 32, 37),
    [System.Drawing.Color]::FromArgb(255, 9, 10, 13),
    90
)
$g.FillRectangle($floorBrush, 0, 620, $width, 460)
$floorBrush.Dispose()

$gridPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(46, 214, 160, 98)), 2
for ($x = -180; $x -lt $width + 200; $x += 96) {
    $g.DrawLine($gridPen, $x, 1080, [int](960 + ($x - 960) * 0.18), 650)
}
for ($y = 650; $y -lt 1080; $y += 56) {
    $g.DrawLine($gridPen, 0, $y, $width, $y)
}
$gridPen.Dispose()

$centerGlow = New-GlowBitmap -Width 940 -Height 500 -Color ([System.Drawing.Color]::FromArgb(255, 255, 183, 74)) -Strength 55
$g.DrawImage($centerGlow, 490, 385, 940, 500)
$centerGlow.Dispose()

$platformBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(230, 36, 35, 38))
$g.FillEllipse($platformBrush, 485, 575, 950, 260)
$platformBrush.Dispose()
$platformPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(175, 255, 212, 118)), 6
$g.DrawEllipse($platformPen, 485, 575, 950, 260)
$platformPen.Dispose()

$titleFont = New-Font -Family 'Microsoft YaHei UI' -Size 128 -Style ([System.Drawing.FontStyle]::Bold)
$subFont = New-Font -Family 'Microsoft YaHei UI' -Size 42 -Style ([System.Drawing.FontStyle]::Regular)
$smallFont = New-Font -Family 'Microsoft YaHei UI' -Size 28 -Style ([System.Drawing.FontStyle]::Bold)
$titleShadow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(180, 0, 0, 0))
$titleBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Rectangle 0, 0, $width, 230),
    [System.Drawing.Color]::FromArgb(255, 255, 241, 184),
    [System.Drawing.Color]::FromArgb(255, 255, 120, 66),
    90
)
$titleText = -join ([char[]](0x5341, 0x4E8C, 0x7B26, 0x5492))
Draw-CenteredText -Graphics $g -Text $titleText -Font $titleFont -Brush $titleShadow -CenterX 960 -Y 103
Draw-CenteredText -Graphics $g -Text $titleText -Font $titleFont -Brush $titleBrush -CenterX 960 -Y 92
$subBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(230, 245, 229, 190))
Draw-CenteredText -Graphics $g -Text 'Collect ancient power. Unlock every talisman ability.' -Font $subFont -Brush $subBrush -CenterX 960 -Y 226
$titleShadow.Dispose()
$titleBrush.Dispose()
$subBrush.Dispose()

$cx = 960
$cy = 603
$rx = 650
$ry = 265
for ($i = 0; $i -lt $Talismans.Count; $i++) {
    $angle = (-90 + $i * 360.0 / $Talismans.Count) * [Math]::PI / 180.0
    $x = $cx + [Math]::Cos($angle) * $rx
    $y = $cy + [Math]::Sin($angle) * $ry
    $size = if ($i -in 0, 4, 10) { 158 } else { 142 }
    $rot = -14 + (($i * 11) % 29)
    Draw-Talisman -Graphics $g -Talisman $Talismans[$i] -X $x -Y $y -Size $size -Angle $rot
}

$linePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(118, 255, 197, 92)), 3
$linePen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.DrawEllipse($linePen, [int]($cx - $rx), [int]($cy - $ry), [int]($rx * 2), [int]($ry * 2))
$linePen.Dispose()

$badgeBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(210, 18, 18, 22))
$badgePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(165, 255, 219, 137)), 3
$g.FillRectangle($badgeBrush, 610, 884, 700, 72)
$g.DrawRectangle($badgePen, 610, 884, 700, 72)
$badgeText = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(245, 255, 236, 191))
Draw-CenteredText -Graphics $g -Text 'REAL MOD TEXTURES - 12 COMPLETE TALISMANS' -Font $smallFont -Brush $badgeText -CenterX 960 -Y 904
$badgeBrush.Dispose()
$badgePen.Dispose()
$badgeText.Dispose()

$vignette = New-GlowBitmap -Width 2220 -Height 1380 -Color ([System.Drawing.Color]::FromArgb(255, 0, 0, 0)) -Strength 28
$g.DrawImage($vignette, -150, -120, 2220, 1380)
$vignette.Dispose()

$titleFont.Dispose()
$subFont.Dispose()
$smallFont.Dispose()
$g.Dispose()

$canvas.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()

Write-Output $OutPath
