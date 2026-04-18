$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$modelPath = Join-Path $PSScriptRoot 'shengzhu_palace_full.geo.json'
$blockbenchTexturePath = Join-Path $PSScriptRoot 'shengzhu_palace.png'
$assetTexturePath = Join-Path $projectRoot 'src/main/resources/assets/chen_mod/textures/entity/shengzhu_palace.png'

$model = Get-Content $modelPath -Raw | ConvertFrom-Json
$geometry = $model.'minecraft:geometry'[0]
$width = [int]$geometry.description.texture_width
$height = [int]$geometry.description.texture_height

function New-Color {
    param([int]$R, [int]$G, [int]$B, [int]$A = 255)
    return [System.Drawing.Color]::FromArgb($A, $R, $G, $B)
}

$palette = @{
    wall_fill         = (New-Color 216 202 103)
    wall_light        = (New-Color 235 224 141)
    wall_shadow       = (New-Color 180 160 72)
    stone_fill        = (New-Color 163 151 79)
    stone_light       = (New-Color 190 178 104)
    stone_shadow      = (New-Color 119 107 54)
    roof_fill         = (New-Color 176 36 26)
    roof_light        = (New-Color 214 70 52)
    roof_shadow       = (New-Color 108 18 13)
    accent_fill       = (New-Color 35 24 18)
    accent_light      = (New-Color 65 46 34)
    accent_shadow     = (New-Color 12 8 6)
    gold_fill         = (New-Color 231 196 81)
    gold_light        = (New-Color 248 219 120)
    gold_shadow       = (New-Color 174 136 46)
    door_fill         = (New-Color 142 28 24)
    door_light        = (New-Color 185 51 44)
    door_shadow       = (New-Color 83 16 13)
}

function Get-StyleName {
    param($BoneName, $Cube)

    $sx = [int][math]::Round([double]$Cube.size[0])
    $sy = [int][math]::Round([double]$Cube.size[1])
    $sz = [int][math]::Round([double]$Cube.size[2])
    $ox = [int][math]::Round([double]$Cube.origin[0])
    $oy = [int][math]::Round([double]$Cube.origin[1])
    $oz = [int][math]::Round([double]$Cube.origin[2])

    if ($BoneName -eq 'foundation_base') { return 'stone' }
    if ($BoneName -eq 'courtyard_floor') { return 'stone' }
    if ($BoneName -eq 'grand_roof' -or $BoneName -eq 'upper_roof') { return 'roof' }
    if ($BoneName -eq 'roof_accents') { return 'accent' }

    if ($BoneName -eq 'front_stairway') {
        if ($sx -eq 32 -and $sy -ge 18 -and $sz -eq 8 -and $oz -le -148) { return 'door' }
        return 'stone'
    }

    if ($BoneName -eq 'front_gate') {
        if ($sx -le 2 -and $sz -le 2 -and $sy -ge 10) { return 'accent' }
        if (($sy -le 4 -and $sx -ge 32) -or ($oy -ge 56)) { return 'roof' }
        return 'wall'
    }

    if ($BoneName -eq 'corner_watchtowers') {
        if ($sx -le 2 -and $sz -le 2) { return 'accent' }
        if ($sy -le 4 -or $oy -ge 66) { return 'roof' }
        return 'wall'
    }

    if ($BoneName -eq 'central_tower') {
        if (($sx -le 4 -and $sz -le 4) -or $oy -ge 224) { return 'accent' }
        if ($sy -le 7 -and $sx -ge 20) { return 'roof' }
        if ($oy -ge 199 -and $sx -le 20) { return 'gold' }
        return 'wall'
    }

    if ($BoneName -match 'pagoda') {
        if (($sx -le 2 -and $sz -le 2) -or ($sy -le 2 -and $sx -le 8 -and $sz -le 6)) { return 'accent' }
        if ($sy -le 4 -or ($sy -le 6 -and $sx -ge 14)) { return 'roof' }
        if ($sx -le 6 -and $sy -ge 10) { return 'gold' }
        return 'wall'
    }

    if ($BoneName -eq 'outer_ring_walls' -or $BoneName -eq 'left_wing_shell' -or $BoneName -eq 'right_wing_shell' -or $BoneName -eq 'main_hall_shell' -or $BoneName -eq 'rear_keep' -or $BoneName -eq 'side_buttresses') {
        if ($sy -le 8 -and $sx -ge 20) { return 'roof' }
        return 'wall'
    }

    return 'wall'
}

function Get-Style {
    param([string]$Name)

    switch ($Name) {
        'stone' { return @{ fill = $palette.stone_fill; light = $palette.stone_light; shadow = $palette.stone_shadow } }
        'roof'  { return @{ fill = $palette.roof_fill; light = $palette.roof_light; shadow = $palette.roof_shadow } }
        'accent'{ return @{ fill = $palette.accent_fill; light = $palette.accent_light; shadow = $palette.accent_shadow } }
        'gold'  { return @{ fill = $palette.gold_fill; light = $palette.gold_light; shadow = $palette.gold_shadow } }
        'door'  { return @{ fill = $palette.door_fill; light = $palette.door_light; shadow = $palette.door_shadow } }
        default { return @{ fill = $palette.wall_fill; light = $palette.wall_light; shadow = $palette.wall_shadow } }
    }
}

function Paint-BoxUv {
    param(
        [System.Drawing.Graphics]$Graphics,
        $Cube,
        [hashtable]$Style
    )

    $u = [int][math]::Round([double]$Cube.uv[0])
    $v = [int][math]::Round([double]$Cube.uv[1])
    $sx = [int][math]::Round([double]$Cube.size[0])
    $sy = [int][math]::Round([double]$Cube.size[1])
    $sz = [int][math]::Round([double]$Cube.size[2])

    $boxWidth = [math]::Max(4, (2 * ($sx + $sz)))
    $boxHeight = [math]::Max(4, ($sy + $sz))

    $fillBrush = [System.Drawing.SolidBrush]::new($Style.fill)
    $lightBrush = [System.Drawing.SolidBrush]::new($Style.light)
    $shadowBrush = [System.Drawing.SolidBrush]::new($Style.shadow)
    $outlinePen = [System.Drawing.Pen]::new($Style.shadow)

    try {
        $Graphics.FillRectangle($fillBrush, $u, $v, $boxWidth, $boxHeight)

        $topBand = [math]::Max(2, [math]::Floor($boxHeight * 0.18))
        $bottomBand = [math]::Max(2, [math]::Floor($boxHeight * 0.14))
        $sideBand = [math]::Max(1, [math]::Floor($boxWidth * 0.04))

        $Graphics.FillRectangle($lightBrush, $u + 1, $v + 1, [math]::Max(1, $boxWidth - 2), $topBand)
        $Graphics.FillRectangle($shadowBrush, $u, $v + $boxHeight - $bottomBand, $boxWidth, $bottomBand)
        $Graphics.FillRectangle($shadowBrush, $u + $boxWidth - $sideBand, $v, $sideBand, $boxHeight)

        if ($boxHeight - 10 -gt 2) {
            $stripePen = [System.Drawing.Pen]::new($Style.shadow)
            try {
                for ($stripe = $u + 6; $stripe -lt ($u + $boxWidth - 6); $stripe += 14) {
                    $Graphics.DrawLine($stripePen, $stripe, $v + 5, $stripe, $v + $boxHeight - 6)
                }
            }
            finally {
                $stripePen.Dispose()
            }
        }

        $Graphics.DrawRectangle($outlinePen, $u, $v, $boxWidth - 1, $boxHeight - 1)
    }
    finally {
        $fillBrush.Dispose()
        $lightBrush.Dispose()
        $shadowBrush.Dispose()
        $outlinePen.Dispose()
    }
}

$bitmap = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)

        foreach ($bone in $geometry.bones) {
            if (-not $bone.PSObject.Properties.Name.Contains('cubes')) {
                continue
            }

            foreach ($cube in $bone.cubes) {
                $styleName = Get-StyleName -BoneName $bone.name -Cube $cube
                $style = Get-Style -Name $styleName
                Paint-BoxUv -Graphics $graphics -Cube $cube -Style $style
            }
        }
    }
    finally {
        $graphics.Dispose()
    }

    $bitmap.Save($blockbenchTexturePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Save($assetTexturePath, [System.Drawing.Imaging.ImageFormat]::Png)
}
finally {
    $bitmap.Dispose()
}

Write-Output "Texture: $blockbenchTexturePath"
Write-Output "Texture asset: $assetTexturePath"
