$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$geoPath = Join-Path $root "shadow_ninja_full.geo.json"
$textureDir = Join-Path $root "textures"

if (-not (Test-Path $textureDir)) {
    New-Item -ItemType Directory -Path $textureDir | Out-Null
}

$geo = Get-Content $geoPath -Raw | ConvertFrom-Json
$geometry = $geo.'minecraft:geometry'[0]
$textureWidth = [int]$geometry.description.texture_width
$textureHeight = [int]$geometry.description.texture_height

function New-Bitmap {
    param(
        [int]$Width,
        [int]$Height,
        [System.Drawing.Color]$Background
    )

    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear($Background)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

    return @{
        Bitmap = $bitmap
        Graphics = $graphics
    }
}

function Get-BoneStyle {
    param([string]$BoneName)

    switch -Regex ($BoneName) {
        "eye" {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(190, 170, 35, 35)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 255, 120, 120)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 220, 35, 35)
            }
        }
        "visor" {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(170, 95, 150, 170)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 78, 188, 214)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 72, 164, 178)
            }
        }
        "head" {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(160, 38, 38, 46)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 92, 92, 110)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 16, 16, 20)
            }
        }
        "belt|loin" {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(170, 135, 45, 52)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 191, 78, 90)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 111, 40, 47)
            }
        }
        "hand|shin|foot" {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(170, 72, 122, 128)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 101, 172, 182)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 88, 131, 138)
            }
        }
        "shoulder|overlay" {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(170, 66, 66, 74)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 120, 120, 132)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 43, 43, 48)
            }
        }
        default {
            return @{
                TemplateFill = [System.Drawing.Color]::FromArgb(170, 33, 33, 39)
                TemplateBorder = [System.Drawing.Color]::FromArgb(255, 110, 110, 120)
                BaseFill = [System.Drawing.Color]::FromArgb(255, 22, 22, 26)
            }
        }
    }
}

function Get-FaceRects {
    param(
        [int]$U,
        [int]$V,
        [int]$X,
        [int]$Y,
        [int]$Z
    )

    return @(
        @{ Face = "left"; Rect = [System.Drawing.Rectangle]::new($U, $V + $Z, $Z, $Y) }
        @{ Face = "front"; Rect = [System.Drawing.Rectangle]::new($U + $Z, $V + $Z, $X, $Y) }
        @{ Face = "right"; Rect = [System.Drawing.Rectangle]::new($U + $Z + $X, $V + $Z, $Z, $Y) }
        @{ Face = "back"; Rect = [System.Drawing.Rectangle]::new($U + $Z + $X + $Z, $V + $Z, $X, $Y) }
        @{ Face = "top"; Rect = [System.Drawing.Rectangle]::new($U + $Z, $V, $X, $Z) }
        @{ Face = "bottom"; Rect = [System.Drawing.Rectangle]::new($U + $Z + $X, $V, $X, $Z) }
    )
}

function Draw-Grid {
    param(
        [System.Drawing.Graphics]$Graphics,
        [int]$Width,
        [int]$Height
    )

    $minorPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 236, 236, 236))
    $majorPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 214, 214, 214))

    for ($x = 0; $x -lt $Width; $x++) {
        $pen = if ($x % 8 -eq 0) { $majorPen } else { $minorPen }
        $Graphics.DrawLine($pen, $x, 0, $x, $Height - 1)
    }

    for ($y = 0; $y -lt $Height; $y++) {
        $pen = if ($y % 8 -eq 0) { $majorPen } else { $minorPen }
        $Graphics.DrawLine($pen, 0, $y, $Width - 1, $y)
    }

    $minorPen.Dispose()
    $majorPen.Dispose()
}

function Fill-Rect {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Rectangle]$Rect,
        [System.Drawing.Color]$Color
    )

    if ($Rect.Width -le 0 -or $Rect.Height -le 0) {
        return
    }

    $brush = New-Object System.Drawing.SolidBrush($Color)
    $Graphics.FillRectangle($brush, $Rect)
    $brush.Dispose()
}

function Outline-Rect {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Rectangle]$Rect,
        [System.Drawing.Color]$Color
    )

    if ($Rect.Width -le 0 -or $Rect.Height -le 0) {
        return
    }

    $pen = New-Object System.Drawing.Pen($Color)
    $Graphics.DrawRectangle($pen, $Rect.X, $Rect.Y, $Rect.Width - 1, $Rect.Height - 1)
    $pen.Dispose()
}

function Draw-FaceAccents {
    param([System.Drawing.Graphics]$Graphics)

    $visorBright = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 104, 218, 226))
    $visorDark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 48, 136, 150))
    $redGlow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 96, 88))
    $red = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 220, 35, 35))

    $Graphics.FillRectangle($visorBright, 43, 25, 5, 1)
    $Graphics.FillRectangle($visorDark, 43, 26, 5, 1)
    $Graphics.FillRectangle($visorDark, 53, 25, 1, 2)

    $Graphics.FillRectangle($redGlow, 61, 25, 2, 1)
    $Graphics.FillRectangle($red, 61, 26, 2, 1)
    $Graphics.FillRectangle($redGlow, 67, 25, 2, 1)
    $Graphics.FillRectangle($red, 67, 26, 2, 1)

    $visorBright.Dispose()
    $visorDark.Dispose()
    $redGlow.Dispose()
    $red.Dispose()
}

$templatePack = New-Bitmap -Width $textureWidth -Height $textureHeight -Background ([System.Drawing.Color]::FromArgb(255, 250, 250, 248))
$basePack = New-Bitmap -Width $textureWidth -Height $textureHeight -Background ([System.Drawing.Color]::Transparent)

Draw-Grid -Graphics $templatePack.Graphics -Width $textureWidth -Height $textureHeight

foreach ($bone in $geometry.bones) {
    if (-not $bone.cubes) {
        continue
    }

    $style = Get-BoneStyle -BoneName $bone.name

    foreach ($cube in $bone.cubes) {
        $u = [int]$cube.uv[0]
        $v = [int]$cube.uv[1]
        $x = [int]$cube.size[0]
        $y = [int]$cube.size[1]
        $z = [int]$cube.size[2]

        foreach ($face in (Get-FaceRects -U $u -V $v -X $x -Y $y -Z $z)) {
            Fill-Rect -Graphics $templatePack.Graphics -Rect $face.Rect -Color $style.TemplateFill
            Outline-Rect -Graphics $templatePack.Graphics -Rect $face.Rect -Color $style.TemplateBorder
            Fill-Rect -Graphics $basePack.Graphics -Rect $face.Rect -Color $style.BaseFill
        }
    }
}

Draw-FaceAccents -Graphics $basePack.Graphics

$accentBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 128, 46, 52))
$basePack.Graphics.FillRectangle($accentBrush, 72, 58, 5, 4)
$basePack.Graphics.FillRectangle($accentBrush, 53, 40, 8, 2)
$accentBrush.Dispose()

$highlightBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 62, 62, 70))
$basePack.Graphics.FillRectangle($highlightBrush, 67, 1, 4, 1)
$basePack.Graphics.FillRectangle($highlightBrush, 67, 9, 4, 1)
$highlightBrush.Dispose()

$templatePath = Join-Path $textureDir "shadow_ninja_uv_template.png"
$basePath = Join-Path $textureDir "shadow_ninja_texture_base.png"

$templatePack.Bitmap.Save($templatePath, [System.Drawing.Imaging.ImageFormat]::Png)
$basePack.Bitmap.Save($basePath, [System.Drawing.Imaging.ImageFormat]::Png)

$templatePack.Graphics.Dispose()
$templatePack.Bitmap.Dispose()
$basePack.Graphics.Dispose()
$basePack.Bitmap.Dispose()

Write-Output $templatePath
Write-Output $basePath
