Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$BasePath = 'C:\Users\20767\.codex\generated_images\019dc849-7b2f-7201-b6bd-df0290abd909\ig_02ec9ebc6095e2f80169eda68e3ba481939e2c27f547dd9be0.png'
$OutPath = Join-Path $Root 'promo_images\shadow_battle_real_talismans.png'
$TextureDir = Join-Path $Root 'src\main\resources\assets\chen_mod\textures\item\talisman'

$Talismans = @(
    @{ Name = 'dog';     File = 'dog_talisman.png';     Color = [System.Drawing.Color]::FromArgb(255, 240, 230, 160) },
    @{ Name = 'dragon';  File = 'dragon_talisman.png';  Color = [System.Drawing.Color]::FromArgb(255, 255, 95, 35) },
    @{ Name = 'horse';   File = 'horse_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 95, 220, 180) },
    @{ Name = 'monkey';  File = 'monkey_talisman.png';  Color = [System.Drawing.Color]::FromArgb(255, 190, 110, 255) },
    @{ Name = 'mouse';   File = 'mouse_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 80, 190, 255) },
    @{ Name = 'ox';      File = 'ox_talisman.png';      Color = [System.Drawing.Color]::FromArgb(255, 255, 210, 70) },
    @{ Name = 'pig';     File = 'pig_talisman.png';     Color = [System.Drawing.Color]::FromArgb(255, 255, 140, 170) },
    @{ Name = 'rabbit';  File = 'rabbit_talisman.png';  Color = [System.Drawing.Color]::FromArgb(255, 120, 255, 120) },
    @{ Name = 'rooster'; File = 'rooster_talisman.png'; Color = [System.Drawing.Color]::FromArgb(255, 255, 180, 70) },
    @{ Name = 'sheep';   File = 'sheep_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 160, 190, 255) },
    @{ Name = 'snake';   File = 'snack_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 80, 255, 170) },
    @{ Name = 'tiger';   File = 'tiger_talisman.png';   Color = [System.Drawing.Color]::FromArgb(255, 255, 245, 115) }
)

function New-GlowBitmap {
    param(
        [int] $Size,
        [System.Drawing.Color] $Color
    )

    $bmp = New-Object System.Drawing.Bitmap $Size, $Size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    for ($i = 0; $i -lt 16; $i++) {
        $ratio = $i / 17.0
        $pad = [int]($ratio * ($Size / 2))
        $alpha = [int](34 * [Math]::Pow(1.0 - $ratio, 2.2))
        $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($alpha, $Color))
        $g.FillEllipse($brush, $pad, $pad, $Size - 2 * $pad, $Size - 2 * $pad)
        $brush.Dispose()
    }

    $g.Dispose()
    return $bmp
}

$base = [System.Drawing.Bitmap]::FromFile($BasePath)
$canvas = New-Object System.Drawing.Bitmap $base.Width, $base.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.DrawImage($base, 0, 0, $base.Width, $base.Height)

$cx = 910
$cy = 882
$rx = 665
$ry = 112
$start = 198
$end = 342
$count = $Talismans.Count

for ($i = 0; $i -lt $count; $i++) {
    $t = $Talismans[$i]
    $angle = ($start + (($end - $start) * $i / ($count - 1))) * [Math]::PI / 180.0
    $x = $cx + [Math]::Cos($angle) * $rx
    $y = $cy + [Math]::Sin($angle) * $ry
    $size = 104
    if ($i -eq 5 -or $i -eq 6) { $size = 118 }
    if ($i -eq 0 -or $i -eq 11) { $size = 92 }

    $texturePath = Join-Path $TextureDir $t.File
    $tex = [System.Drawing.Bitmap]::FromFile($texturePath)

    $glow = New-GlowBitmap -Size ([int]($size * 1.72)) -Color $t.Color
    $gx = [int]($x - $glow.Width / 2)
    $gy = [int]($y - $glow.Height / 2)
    $g.DrawImage($glow, $gx, $gy, $glow.Width, $glow.Height)

    $shadowBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(175, 0, 0, 0))
    $g.FillEllipse($shadowBrush, [int]($x - $size * 0.58), [int]($y - $size * 0.50), [int]($size * 1.16), [int]($size * 1.00))
    $shadowBrush.Dispose()

    $backBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(55, 255, 235, 180))
    $g.FillEllipse($backBrush, [int]($x - $size * 0.54), [int]($y - $size * 0.54), [int]($size * 1.08), [int]($size * 1.08))
    $backBrush.Dispose()

    $oldTransform = $g.Transform
    $matrix = New-Object System.Drawing.Drawing2D.Matrix
    $matrix.Translate([float]$x, [float]$y)
    $matrix.Rotate([float](-18 + 36 * $i / ($count - 1)))
    $g.Transform = $matrix
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($tex, [int](-$size / 2), [int](-$size / 2), $size, $size)
    $g.Transform = $oldTransform
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(145, $t.Color)), 2
    $g.DrawEllipse($pen, [int]($x - $size * 0.62), [int]($y - $size * 0.62), [int]($size * 1.24), [int]($size * 1.24))
    $pen.Dispose()

    $rand = New-Object System.Random ($i + 20260426)
    for ($p = 0; $p -lt 10; $p++) {
        $px = $x + $rand.Next(-44, 45)
        $py = $y + $rand.Next(-40, 41)
        $ps = $rand.Next(2, 5)
        $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($rand.Next(75, 145), $t.Color))
        $g.FillEllipse($brush, [int]$px, [int]$py, $ps, $ps)
        $brush.Dispose()
    }

    $tex.Dispose()
    $glow.Dispose()
}

$arcPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(65, 210, 160, 255)), 2
$arcPen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.DrawArc($arcPen, $cx - $rx, $cy - $ry, $rx * 2, $ry * 2, $start, $end - $start)
$arcPen.Dispose()

$g.Dispose()
$base.Dispose()

$canvas.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()

Write-Output $OutPath

$ShowcaseBasePath = 'C:\Users\20767\.codex\generated_images\019dc849-7b2f-7201-b6bd-df0290abd909\ig_02ec9ebc6095e2f80169eda61ef8e4819395fc1b521f3f99d9.png'
$ShowcaseOutPath = Join-Path $Root 'promo_images\talisman_showcase_real_talismans.png'
$ShowcaseLayout = @(
    @{ X = 560;  Y = 330; Size = 142; Angle = -20 },
    @{ X = 760;  Y = 250; Size = 154; Angle = 12 },
    @{ X = 1000; Y = 222; Size = 154; Angle = 8 },
    @{ X = 1222; Y = 330; Size = 150; Angle = 18 },
    @{ X = 1360; Y = 510; Size = 145; Angle = -18 },
    @{ X = 1268; Y = 668; Size = 145; Angle = 15 },
    @{ X = 1108; Y = 742; Size = 150; Angle = -8 },
    @{ X = 910;  Y = 760; Size = 150; Angle = 10 },
    @{ X = 724;  Y = 720; Size = 148; Angle = -14 },
    @{ X = 585;  Y = 612; Size = 148; Angle = 18 },
    @{ X = 512;  Y = 492; Size = 142; Angle = -15 },
    @{ X = 620;  Y = 405; Size = 142; Angle = 14 }
)

$base = [System.Drawing.Bitmap]::FromFile($ShowcaseBasePath)
$canvas = New-Object System.Drawing.Bitmap $base.Width, $base.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.DrawImage($base, 0, 0, $base.Width, $base.Height)

for ($i = 0; $i -lt $Talismans.Count; $i++) {
    $t = $Talismans[$i]
    $slot = $ShowcaseLayout[$i]
    $x = [double]$slot.X
    $y = [double]$slot.Y
    $size = [int]$slot.Size

    $texturePath = Join-Path $TextureDir $t.File
    $tex = [System.Drawing.Bitmap]::FromFile($texturePath)
    $glow = New-GlowBitmap -Size ([int]($size * 1.55)) -Color $t.Color

    $g.DrawImage($glow, [int]($x - $glow.Width / 2), [int]($y - $glow.Height / 2), $glow.Width, $glow.Height)

    $shadowBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(170, 0, 0, 0))
    $g.FillEllipse($shadowBrush, [int]($x - $size * 0.54), [int]($y - $size * 0.54), [int]($size * 1.08), [int]($size * 1.08))
    $shadowBrush.Dispose()

    $ringPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(150, $t.Color)), 4
    $g.DrawEllipse($ringPen, [int]($x - $size * 0.59), [int]($y - $size * 0.59), [int]($size * 1.18), [int]($size * 1.18))
    $ringPen.Dispose()

    $oldTransform = $g.Transform
    $matrix = New-Object System.Drawing.Drawing2D.Matrix
    $matrix.Translate([float]$x, [float]$y)
    $matrix.Rotate([float]$slot.Angle)
    $g.Transform = $matrix
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($tex, [int](-$size / 2), [int](-$size / 2), $size, $size)
    $g.Transform = $oldTransform
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $tex.Dispose()
    $glow.Dispose()
}

$g.Dispose()
$base.Dispose()
$canvas.Save($ShowcaseOutPath, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()

Write-Output $ShowcaseOutPath
