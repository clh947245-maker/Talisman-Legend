$ErrorActionPreference = 'Stop'

$TextureWidth = 2048
$TextureHeight = 4096
$PartsDir = $PSScriptRoot
$FullModelPath = Join-Path $PartsDir 'new_palace_full.geo.json'

function New-Cube {
    param(
        [object[]]$Origin,
        [object[]]$Size,
        [object[]]$Uv,
        [object[]]$Pivot = $null,
        [object[]]$Rotation = $null,
        [switch]$Mirror
    )

    $cube = [ordered]@{
        origin = $Origin
        size = $Size
        uv = $Uv
    }

    if ($null -ne $Pivot) {
        $cube.pivot = $Pivot
    }

    if ($null -ne $Rotation) {
        $cube.rotation = $Rotation
    }

    if ($Mirror.IsPresent) {
        $cube.mirror = $true
    }

    return $cube
}

function New-Bone {
    param(
        [string]$Name,
        [string]$Parent = 'root',
        [object[]]$Pivot,
        [object[]]$Cubes = @()
    )

    $bone = [ordered]@{
        name = $Name
        pivot = $Pivot
    }

    if (-not [string]::IsNullOrWhiteSpace($Parent)) {
        $bone.parent = $Parent
    }

    if ($Cubes.Count -gt 0) {
        $bone.cubes = $Cubes
    }

    return $bone
}

function New-PartFile {
    param(
        [string]$Identifier,
        [object[]]$Bones
    )

    return [ordered]@{
        format_version = '1.16.0'
        'minecraft:geometry' = @(
            [ordered]@{
                description = [ordered]@{
                    identifier = $Identifier
                    texture_width = $TextureWidth
                    texture_height = $TextureHeight
                }
                bones = $Bones
            }
        )
    }
}

function Write-JsonFile {
    param(
        [string]$Path,
        [object]$Data
    )

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    $json = $Data | ConvertTo-Json -Depth 100
    [System.IO.File]::WriteAllText($Path, $json)
}

function Add-Cubes {
    param(
        [System.Collections.ArrayList]$Target,
        [object[]]$Cubes
    )

    foreach ($cube in $Cubes) {
        [void]$Target.Add($cube)
    }
}

function Add-NorthSouthWall {
    param(
        [System.Collections.ArrayList]$Target,
        [double]$CenterX,
        [double]$BaseY,
        [double]$WallZ,
        [double]$Width,
        [double]$Height,
        [double]$Thickness,
        [double]$DoorWidth = 0,
        [double]$DoorHeight = 0,
        [object[]]$Uv = @(0, 0)
    )

    $leftX = $CenterX - ($Width / 2)
    if ($DoorWidth -le 0 -or $DoorHeight -le 0) {
        [void]$Target.Add((New-Cube -Origin @($leftX, $BaseY, $WallZ) -Size @($Width, $Height, $Thickness) -Uv $Uv))
        return
    }

    $sideWidth = ($Width - $DoorWidth) / 2
    if ($sideWidth -gt 0) {
        [void]$Target.Add((New-Cube -Origin @($leftX, $BaseY, $WallZ) -Size @($sideWidth, $Height, $Thickness) -Uv $Uv))
        [void]$Target.Add((New-Cube -Origin @(($CenterX + ($DoorWidth / 2)), $BaseY, $WallZ) -Size @($sideWidth, $Height, $Thickness) -Uv @((($Uv[0] + 20)), $Uv[1])))
    }

    $lintelHeight = $Height - $DoorHeight
    if ($lintelHeight -gt 0) {
        [void]$Target.Add((New-Cube -Origin @(($CenterX - ($DoorWidth / 2)), ($BaseY + $DoorHeight), $WallZ) -Size @($DoorWidth, $lintelHeight, $Thickness) -Uv @((($Uv[0] + 40)), $Uv[1])))
    }
}

function Add-WestEastWall {
    param(
        [System.Collections.ArrayList]$Target,
        [double]$WallX,
        [double]$BaseY,
        [double]$CenterZ,
        [double]$Depth,
        [double]$Height,
        [double]$Thickness,
        [double]$DoorWidth = 0,
        [double]$DoorHeight = 0,
        [object[]]$Uv = @(0, 0)
    )

    $frontZ = $CenterZ - ($Depth / 2)
    if ($DoorWidth -le 0 -or $DoorHeight -le 0) {
        [void]$Target.Add((New-Cube -Origin @($WallX, $BaseY, $frontZ) -Size @($Thickness, $Height, $Depth) -Uv $Uv))
        return
    }

    $sideDepth = ($Depth - $DoorWidth) / 2
    if ($sideDepth -gt 0) {
        [void]$Target.Add((New-Cube -Origin @($WallX, $BaseY, $frontZ) -Size @($Thickness, $Height, $sideDepth) -Uv $Uv))
        [void]$Target.Add((New-Cube -Origin @($WallX, $BaseY, ($CenterZ + ($DoorWidth / 2))) -Size @($Thickness, $Height, $sideDepth) -Uv @((($Uv[0] + 20)), $Uv[1])))
    }

    $lintelHeight = $Height - $DoorHeight
    if ($lintelHeight -gt 0) {
        [void]$Target.Add((New-Cube -Origin @($WallX, ($BaseY + $DoorHeight), ($CenterZ - ($DoorWidth / 2))) -Size @($Thickness, $lintelHeight, $DoorWidth) -Uv @((($Uv[0] + 40)), $Uv[1])))
    }
}

function New-HallShell {
    param(
        [double]$CenterX,
        [double]$BaseY,
        [double]$CenterZ,
        [double]$Width,
        [double]$Depth,
        [double]$Height,
        [double]$WallThickness = 16,
        [double]$FloorThickness = 16,
        [double]$CeilingThickness = 16,
        [double]$NorthDoorWidth = 0,
        [double]$NorthDoorHeight = 0,
        [double]$SouthDoorWidth = 0,
        [double]$SouthDoorHeight = 0,
        [double]$WestDoorWidth = 0,
        [double]$WestDoorHeight = 0,
        [double]$EastDoorWidth = 0,
        [double]$EastDoorHeight = 0,
        [object[]]$Uv = @(0, 0)
    )

    $halfWidth = $Width / 2
    $halfDepth = $Depth / 2
    $cubes = [System.Collections.ArrayList]::new()

    [void]$cubes.Add((New-Cube -Origin @(($CenterX - $halfWidth), $BaseY, ($CenterZ - $halfDepth)) -Size @($Width, $FloorThickness, $Depth) -Uv $Uv))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - $halfWidth), ($BaseY + $Height - $CeilingThickness), ($CenterZ - $halfDepth)) -Size @($Width, $CeilingThickness, $Depth) -Uv @((($Uv[0] + 60)), $Uv[1])))

    Add-WestEastWall -Target $cubes -WallX ($CenterX - $halfWidth) -BaseY ($BaseY + $FloorThickness) -CenterZ $CenterZ -Depth $Depth -Height ($Height - $FloorThickness - $CeilingThickness) -Thickness $WallThickness -DoorWidth $WestDoorWidth -DoorHeight $WestDoorHeight -Uv @($Uv[0], (($Uv[1] + 20)))
    Add-WestEastWall -Target $cubes -WallX ($CenterX + $halfWidth - $WallThickness) -BaseY ($BaseY + $FloorThickness) -CenterZ $CenterZ -Depth $Depth -Height ($Height - $FloorThickness - $CeilingThickness) -Thickness $WallThickness -DoorWidth $EastDoorWidth -DoorHeight $EastDoorHeight -Uv @((($Uv[0] + 20)), (($Uv[1] + 20)))
    Add-NorthSouthWall -Target $cubes -CenterX $CenterX -BaseY ($BaseY + $FloorThickness) -WallZ ($CenterZ - $halfDepth) -Width $Width -Height ($Height - $FloorThickness - $CeilingThickness) -Thickness $WallThickness -DoorWidth $NorthDoorWidth -DoorHeight $NorthDoorHeight -Uv @((($Uv[0] + 40)), (($Uv[1] + 20)))
    Add-NorthSouthWall -Target $cubes -CenterX $CenterX -BaseY ($BaseY + $FloorThickness) -WallZ ($CenterZ + $halfDepth - $WallThickness) -Width $Width -Height ($Height - $FloorThickness - $CeilingThickness) -Thickness $WallThickness -DoorWidth $SouthDoorWidth -DoorHeight $SouthDoorHeight -Uv @((($Uv[0] + 60)), (($Uv[1] + 20)))

    return $cubes.ToArray()
}

function New-RoofCorners {
    param(
        [double]$CenterX,
        [double]$CenterZ,
        [double]$Y,
        [double]$Width,
        [double]$Depth,
        [double]$SizeX,
        [double]$SizeZ,
        [double]$RotationZ,
        [object[]]$Uv = @(0, 0)
    )

    $halfWidth = $Width / 2
    $halfDepth = $Depth / 2
    return @(
        (New-Cube -Origin @((($CenterX - $halfWidth) - $SizeX + 16), $Y, (($CenterZ - $halfDepth) - $SizeZ + 16)) -Size @($SizeX, 8, $SizeZ) -Uv $Uv -Pivot @((($CenterX - $halfWidth) + 16), ($Y + 4), (($CenterZ - $halfDepth) + 16)) -Rotation @(0, 0, $RotationZ)),
        (New-Cube -Origin @((($CenterX + $halfWidth) - 16), $Y, (($CenterZ - $halfDepth) - $SizeZ + 16)) -Size @($SizeX, 8, $SizeZ) -Uv @((($Uv[0] + 16)), $Uv[1]) -Pivot @((($CenterX + $halfWidth) - 16), ($Y + 4), (($CenterZ - $halfDepth) + 16)) -Rotation @(0, 0, -$RotationZ)),
        (New-Cube -Origin @((($CenterX - $halfWidth) - $SizeX + 16), $Y, (($CenterZ + $halfDepth) - 16)) -Size @($SizeX, 8, $SizeZ) -Uv @($Uv[0], (($Uv[1] + 16))) -Pivot @((($CenterX - $halfWidth) + 16), ($Y + 4), (($CenterZ + $halfDepth) - 16)) -Rotation @(0, 0, -$RotationZ)),
        (New-Cube -Origin @((($CenterX + $halfWidth) - 16), $Y, (($CenterZ + $halfDepth) - 16)) -Size @($SizeX, 8, $SizeZ) -Uv @((($Uv[0] + 16)), (($Uv[1] + 16))) -Pivot @((($CenterX + $halfWidth) - 16), ($Y + 4), (($CenterZ + $halfDepth) - 16)) -Rotation @(0, 0, $RotationZ))
    )
}

function New-RoofTier {
    param(
        [double]$CenterX,
        [double]$CenterZ,
        [double]$BaseY,
        [double]$Width,
        [double]$Depth,
        [object[]]$Uv = @(0, 0)
    )

    $cubes = [System.Collections.ArrayList]::new()
    $tier1Width = $Width + 156
    $tier1Depth = $Depth + 136
    $tier2Width = $Width + 108
    $tier2Depth = $Depth + 92
    $tier3Width = $Width + 64
    $tier3Depth = $Depth + 56
    $tier4Width = $Width + 16
    $tier4Depth = $Depth + 12
    $tier5Width = [Math]::Max(64, [int]($Width - 36))
    $tier5Depth = [Math]::Max(64, [int]($Depth - 36))

    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($tier1Width / 2)), $BaseY, ($CenterZ - ($tier1Depth / 2))) -Size @($tier1Width, 6, $tier1Depth) -Uv $Uv))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($tier2Width / 2)), ($BaseY + 5), ($CenterZ - ($tier2Depth / 2))) -Size @($tier2Width, 6, $tier2Depth) -Uv @((($Uv[0] + 50)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($tier3Width / 2)), ($BaseY + 11), ($CenterZ - ($tier3Depth / 2))) -Size @($tier3Width, 7, $tier3Depth) -Uv @((($Uv[0] + 104)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($tier4Width / 2)), ($BaseY + 18), ($CenterZ - ($tier4Depth / 2))) -Size @($tier4Width, 7, $tier4Depth) -Uv @((($Uv[0] + 158)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($tier5Width / 2)), ($BaseY + 25), ($CenterZ - ($tier5Depth / 2))) -Size @($tier5Width, 8, $tier5Depth) -Uv @((($Uv[0] + 212)), $Uv[1])))

    Add-Cubes -Target $cubes -Cubes (New-RoofCorners -CenterX $CenterX -CenterZ $CenterZ -Y ($BaseY + 1) -Width $tier1Width -Depth $tier1Depth -SizeX 48 -SizeZ 32 -RotationZ 34 -Uv @((($Uv[0] + 112)), (($Uv[1] + 52))))
    Add-Cubes -Target $cubes -Cubes (New-RoofCorners -CenterX $CenterX -CenterZ $CenterZ -Y ($BaseY + 12) -Width $tier3Width -Depth $tier3Depth -SizeX 34 -SizeZ 22 -RotationZ 28 -Uv @((($Uv[0] + 164)), (($Uv[1] + 52))))
    Add-Cubes -Target $cubes -Cubes (New-RoofCorners -CenterX $CenterX -CenterZ $CenterZ -Y ($BaseY + 26) -Width $tier5Width -Depth $tier5Depth -SizeX 22 -SizeZ 16 -RotationZ 22 -Uv @((($Uv[0] + 212)), (($Uv[1] + 52))))
    Add-Cubes -Target $cubes -Cubes (New-RidgeDecorations -CenterX $CenterX -CenterZ $CenterZ -BaseY $BaseY -Width $Width -Depth $Depth -Uv @((($Uv[0] + 10)), (($Uv[1] + 96))))

    return $cubes.ToArray()
}

function New-RidgeDecorations {
    param(
        [double]$CenterX,
        [double]$CenterZ,
        [double]$BaseY,
        [double]$Width,
        [double]$Depth,
        [object[]]$Uv = @(0, 0)
    )

    $cubes = [System.Collections.ArrayList]::new()
    $ridgeWidth = [Math]::Max(32, [int]($Width * 0.11))
    $ridgeDepth = [Math]::Max(56, [int]($Depth - 68))
    $crestWidth = [Math]::Max(52, [int]($Width * 0.20))
    $beastWidth = [Math]::Max(22, [int]($Width * 0.06))
    $beastDepth = [Math]::Max(26, [int]($Width * 0.07))
    $sideRidgeLength = [Math]::Max(58, [int]($Width * 0.22))
    $crestCount = [Math]::Max(3, [int][Math]::Floor($ridgeDepth / 56))
    $crestStep = if ($crestCount -gt 1) { ($ridgeDepth - 34) / ($crestCount - 1) } else { 0 }

    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($ridgeWidth / 2)), ($BaseY + 34), ($CenterZ - ($ridgeDepth / 2))) -Size @($ridgeWidth, 18, $ridgeDepth) -Uv $Uv))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($crestWidth / 2)), ($BaseY + 48), ($CenterZ - ($ridgeDepth / 2) + 8)) -Size @($crestWidth, 12, ($ridgeDepth - 16)) -Uv @((($Uv[0] + 34)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - 16), ($BaseY + 58), ($CenterZ - 16)) -Size @(32, 22, 32) -Uv @((($Uv[0] + 82)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - 10), ($BaseY + 80), ($CenterZ - 10)) -Size @(20, 18, 20) -Uv @((($Uv[0] + 118)), $Uv[1])))

    for ($i = 0; $i -lt $crestCount; $i++) {
        $ornamentZ = ($CenterZ - ($ridgeDepth / 2)) + 17 + ($i * $crestStep)
        [void]$cubes.Add((New-Cube -Origin @(($CenterX - 5), ($BaseY + 60), $ornamentZ) -Size @(10, 16, 10) -Uv @((($Uv[0] + 144)), $Uv[1])))
        [void]$cubes.Add((New-Cube -Origin @(($CenterX - 3), ($BaseY + 76), ($ornamentZ + 2)) -Size @(6, 10, 6) -Uv @((($Uv[0] + 158)), $Uv[1])))
    }

    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($beastWidth / 2)), ($BaseY + 62), (($CenterZ - ($ridgeDepth / 2)) - 16)) -Size @($beastWidth, 28, ($beastDepth + 6)) -Uv @((($Uv[0] + 172)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - (($beastWidth + 6) / 2)), ($BaseY + 86), (($CenterZ - ($ridgeDepth / 2)) - 6)) -Size @(($beastWidth + 6), 22, 16) -Uv @((($Uv[0] + 202)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - 7), ($BaseY + 98), (($CenterZ - ($ridgeDepth / 2)) - 18)) -Size @(14, 28, 14) -Uv @((($Uv[0] + 228)), $Uv[1])))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($beastWidth / 2)), ($BaseY + 62), (($CenterZ + ($ridgeDepth / 2)) - ($beastDepth - 4))) -Size @($beastWidth, 28, ($beastDepth + 6)) -Uv @((($Uv[0] + 172)), (($Uv[1] + 22)))))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - (($beastWidth + 6) / 2)), ($BaseY + 86), (($CenterZ + ($ridgeDepth / 2)) - 10)) -Size @(($beastWidth + 6), 22, 16) -Uv @((($Uv[0] + 202)), (($Uv[1] + 22)))))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - 7), ($BaseY + 98), (($CenterZ + ($ridgeDepth / 2)) - 2)) -Size @(14, 28, 14) -Uv @((($Uv[0] + 228)), (($Uv[1] + 22)))))

    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($sideRidgeLength + 12)), ($BaseY + 44), (($CenterZ - ($ridgeDepth / 2)) + 24)) -Size @($sideRidgeLength, 12, 16) -Uv @($Uv[0], (($Uv[1] + 28))) -Pivot @(($CenterX - 10), ($BaseY + 50), (($CenterZ - ($ridgeDepth / 2)) + 32)) -Rotation @(0, 0, 34)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX + 12), ($BaseY + 44), (($CenterZ - ($ridgeDepth / 2)) + 24)) -Size @($sideRidgeLength, 12, 16) -Uv @((($Uv[0] + 40)), (($Uv[1] + 28))) -Pivot @(($CenterX + 10), ($BaseY + 50), (($CenterZ - ($ridgeDepth / 2)) + 32)) -Rotation @(0, 0, -34)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($sideRidgeLength + 12)), ($BaseY + 56), (($CenterZ - ($ridgeDepth / 2)) + 34)) -Size @((($sideRidgeLength - 18)), 10, 12) -Uv @((($Uv[0] + 80)), (($Uv[1] + 28))) -Pivot @(($CenterX - 10), ($BaseY + 61), (($CenterZ - ($ridgeDepth / 2)) + 42)) -Rotation @(0, 0, 28)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX + 12), ($BaseY + 56), (($CenterZ - ($ridgeDepth / 2)) + 34)) -Size @((($sideRidgeLength - 18)), 10, 12) -Uv @((($Uv[0] + 116)), (($Uv[1] + 28))) -Pivot @(($CenterX + 10), ($BaseY + 61), (($CenterZ - ($ridgeDepth / 2)) + 42)) -Rotation @(0, 0, -28)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($sideRidgeLength + 12)), ($BaseY + 44), (($CenterZ + ($ridgeDepth / 2)) - 40)) -Size @($sideRidgeLength, 12, 16) -Uv @((($Uv[0] + 152)), (($Uv[1] + 28))) -Pivot @(($CenterX - 10), ($BaseY + 50), (($CenterZ + ($ridgeDepth / 2)) - 32)) -Rotation @(0, 0, -34)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX + 12), ($BaseY + 44), (($CenterZ + ($ridgeDepth / 2)) - 40)) -Size @($sideRidgeLength, 12, 16) -Uv @((($Uv[0] + 192)), (($Uv[1] + 28))) -Pivot @(($CenterX + 10), ($BaseY + 50), (($CenterZ + ($ridgeDepth / 2)) - 32)) -Rotation @(0, 0, 34)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($sideRidgeLength + 12)), ($BaseY + 56), (($CenterZ + ($ridgeDepth / 2)) - 50)) -Size @((($sideRidgeLength - 18)), 10, 12) -Uv @((($Uv[0] + 80)), (($Uv[1] + 44))) -Pivot @(($CenterX - 10), ($BaseY + 61), (($CenterZ + ($ridgeDepth / 2)) - 42)) -Rotation @(0, 0, -28)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX + 12), ($BaseY + 56), (($CenterZ + ($ridgeDepth / 2)) - 50)) -Size @((($sideRidgeLength - 18)), 10, 12) -Uv @((($Uv[0] + 116)), (($Uv[1] + 44))) -Pivot @(($CenterX + 10), ($BaseY + 61), (($CenterZ + ($ridgeDepth / 2)) - 42)) -Rotation @(0, 0, 28)))

    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($beastWidth + 14)), ($BaseY + 60), (($CenterZ - ($ridgeDepth / 2)) + 22)) -Size @($beastWidth, 16, 16) -Uv @((($Uv[0] + 232)), (($Uv[1] + 28))) -Pivot @(($CenterX - 12), ($BaseY + 68), (($CenterZ - ($ridgeDepth / 2)) + 30)) -Rotation @(0, 0, 36)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX + 14), ($BaseY + 60), (($CenterZ - ($ridgeDepth / 2)) + 22)) -Size @($beastWidth, 16, 16) -Uv @((($Uv[0] + 252)), (($Uv[1] + 28))) -Pivot @(($CenterX + 12), ($BaseY + 68), (($CenterZ - ($ridgeDepth / 2)) + 30)) -Rotation @(0, 0, -36)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($beastWidth + 14)), ($BaseY + 60), (($CenterZ + ($ridgeDepth / 2)) - 38)) -Size @($beastWidth, 16, 16) -Uv @((($Uv[0] + 232)), (($Uv[1] + 46))) -Pivot @(($CenterX - 12), ($BaseY + 68), (($CenterZ + ($ridgeDepth / 2)) - 30)) -Rotation @(0, 0, -36)))
    [void]$cubes.Add((New-Cube -Origin @(($CenterX + 14), ($BaseY + 60), (($CenterZ + ($ridgeDepth / 2)) - 38)) -Size @($beastWidth, 16, 16) -Uv @((($Uv[0] + 252)), (($Uv[1] + 46))) -Pivot @(($CenterX + 12), ($BaseY + 68), (($CenterZ + ($ridgeDepth / 2)) - 30)) -Rotation @(0, 0, 36)))

    return $cubes.ToArray()
}

function New-StairRun {
    param(
        [double]$CenterX,
        [double]$BaseY,
        [double]$StartZ,
        [double]$StepWidth,
        [double]$StepDepth,
        [double]$StepHeight,
        [int]$Steps,
        [object[]]$Uv = @(0, 0)
    )

    $cubes = [System.Collections.ArrayList]::new()
    for ($i = 0; $i -lt $Steps; $i++) {
        [void]$cubes.Add((New-Cube -Origin @(($CenterX - ($StepWidth / 2)), ($BaseY + ($i * $StepHeight)), ($StartZ + ($i * $StepDepth))) -Size @($StepWidth, $StepHeight, $StepDepth) -Uv @((($Uv[0] + ($i * 8))), $Uv[1])))
    }

    return $cubes.ToArray()
}

$parts = @()

$foundationCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $foundationCubes -Cubes @(
    (New-Cube -Origin @(-640, -32, -848) -Size @(1280, 32, 1696) -Uv @(0, 0)),
    (New-Cube -Origin @(-560, 0, -768) -Size @(1120, 16, 1536) -Uv @(0, 220)),
    (New-Cube -Origin @(-448, 16, -448) -Size @(896, 16, 960) -Uv @(0, 420)),
    (New-Cube -Origin @(-272, 32, -112) -Size @(544, 16, 576) -Uv @(0, 620)),
    (New-Cube -Origin @(-560, 16, -208) -Size @(208, 16, 608) -Uv @(940, 420)),
    (New-Cube -Origin @(352, 16, -208) -Size @(208, 16, 608) -Uv @(1180, 420)),
    (New-Cube -Origin @(-336, 32, 448) -Size @(672, 16, 272) -Uv @(600, 620)),
    (New-Cube -Origin @(-256, 16, -640) -Size @(512, 16, 176) -Uv @(940, 620))
)
$parts += [ordered]@{
    file = 'foundation_base.json'
    bone = (New-Bone -Name 'foundation_base' -Pivot @(0, 0, 0) -Cubes $foundationCubes.ToArray())
}

$courtyardCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $courtyardCubes -Cubes @(
    (New-Cube -Origin @(-416, 32, -672) -Size @(832, 8, 448) -Uv @(0, 860)),
    (New-Cube -Origin @(-256, 40, -240) -Size @(512, 8, 432) -Uv @(0, 940)),
    (New-Cube -Origin @(-544, 32, -208) -Size @(176, 8, 608) -Uv @(540, 940)),
    (New-Cube -Origin @(368, 32, -208) -Size @(176, 8, 608) -Uv @(730, 940)),
    (New-Cube -Origin @(-304, 40, 448) -Size @(608, 8, 208) -Uv @(920, 940)),
    (New-Cube -Origin @(-96, 40, -96) -Size @(192, 12, 192) -Uv @(0, 1040)),
    (New-Cube -Origin @(-176, 40, 256) -Size @(352, 8, 96) -Uv @(220, 1040))
)
$parts += [ordered]@{
    file = 'courtyard_floor.json'
    bone = (New-Bone -Name 'courtyard_floor' -Pivot @(0, 32, 0) -Cubes $courtyardCubes.ToArray())
}

$outerWallCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $outerWallCubes -Cubes @(
    (New-Cube -Origin @(-640, 32, -848) -Size @(32, 224, 1696) -Uv @(0, 1120)),
    (New-Cube -Origin @(608, 32, -848) -Size @(32, 224, 1696) -Uv @(40, 1120)),
    (New-Cube -Origin @(-640, 32, 816) -Size @(1280, 224, 32) -Uv @(80, 1120)),
    (New-Cube -Origin @(-640, 32, -848) -Size @(448, 224, 32) -Uv @(80, 1380)),
    (New-Cube -Origin @(192, 32, -848) -Size @(448, 224, 32) -Uv @(550, 1380)),
    (New-Cube -Origin @(-656, 256, -864) -Size @(48, 24, 1728) -Uv @(0, 1640)),
    (New-Cube -Origin @(608, 256, -864) -Size @(48, 24, 1728) -Uv @(52, 1640)),
    (New-Cube -Origin @(-656, 256, 816) -Size @(1312, 24, 48) -Uv @(104, 1640)),
    (New-Cube -Origin @(-656, 256, -864) -Size @(464, 24, 48) -Uv @(104, 1820)),
    (New-Cube -Origin @(192, 256, -864) -Size @(464, 24, 48) -Uv @(580, 1820))
)
$parts += [ordered]@{
    file = 'outer_ring_walls.json'
    bone = (New-Bone -Name 'outer_ring_walls' -Pivot @(0, 32, 0) -Cubes $outerWallCubes.ToArray())
}

$frontGateCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $frontGateCubes -Cubes @(
    (New-Cube -Origin @(-224, 32, -832) -Size @(128, 320, 176) -Uv @(0, 1900)),
    (New-Cube -Origin @(96, 32, -832) -Size @(128, 320, 176) -Uv @(140, 1900)),
    (New-Cube -Origin @(-128, 32, -784) -Size @(32, 160, 128) -Uv @(280, 1900)),
    (New-Cube -Origin @(96, 32, -784) -Size @(32, 160, 128) -Uv @(320, 1900)),
    (New-Cube -Origin @(-96, 176, -784) -Size @(192, 32, 128) -Uv @(360, 1900)),
    (New-Cube -Origin @(-160, 256, -816) -Size @(320, 112, 144) -Uv @(560, 1900)),
    (New-Cube -Origin @(-320, 32, -704) -Size @(96, 208, 48) -Uv @(890, 1900)),
    (New-Cube -Origin @(224, 32, -704) -Size @(96, 208, 48) -Uv @(990, 1900)),
    (New-Cube -Origin @(-80, 224, -688) -Size @(160, 16, 64) -Uv @(1090, 1900)),
    (New-Cube -Origin @(-176, 368, -800) -Size @(352, 16, 112) -Uv @(1260, 1900))
)
$parts += [ordered]@{
    file = 'front_gate.json'
    bone = (New-Bone -Name 'front_gate' -Pivot @(0, 32, -760) -Cubes $frontGateCubes.ToArray())
}

$frontStairCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $frontStairCubes -Cubes (New-StairRun -CenterX 0 -BaseY -32 -StartZ -976 -StepWidth 352 -StepDepth 32 -StepHeight 8 -Steps 8 -Uv @(0, 2080))
Add-Cubes -Target $frontStairCubes -Cubes (New-StairRun -CenterX -456 -BaseY 16 -StartZ -320 -StepWidth 128 -StepDepth 24 -StepHeight 8 -Steps 3 -Uv @(120, 2080))
Add-Cubes -Target $frontStairCubes -Cubes (New-StairRun -CenterX 456 -BaseY 16 -StartZ -320 -StepWidth 128 -StepDepth 24 -StepHeight 8 -Steps 3 -Uv @(240, 2080))
Add-Cubes -Target $frontStairCubes -Cubes (New-StairRun -CenterX 0 -BaseY 32 -StartZ 400 -StepWidth 224 -StepDepth 24 -StepHeight 8 -Steps 2 -Uv @(320, 2080))
$parts += [ordered]@{
    file = 'front_stairway.json'
    bone = (New-Bone -Name 'front_stairway' -Pivot @(0, 0, -640) -Cubes $frontStairCubes.ToArray())
}

$towerCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $towerCubes -Cubes @(
    (New-Cube -Origin @(-592, 32, -800) -Size @(96, 352, 96) -Uv @(0, 2160)),
    (New-Cube -Origin @(496, 32, -800) -Size @(96, 352, 96) -Uv @(110, 2160)),
    (New-Cube -Origin @(-592, 32, 704) -Size @(96, 352, 96) -Uv @(220, 2160)),
    (New-Cube -Origin @(496, 32, 704) -Size @(96, 352, 96) -Uv @(330, 2160)),
    (New-Cube -Origin @(-608, 384, -816) -Size @(128, 24, 128) -Uv @(440, 2160)),
    (New-Cube -Origin @(480, 384, -816) -Size @(128, 24, 128) -Uv @(580, 2160)),
    (New-Cube -Origin @(-608, 384, 688) -Size @(128, 24, 128) -Uv @(720, 2160)),
    (New-Cube -Origin @(480, 384, 688) -Size @(128, 24, 128) -Uv @(860, 2160))
)
$parts += [ordered]@{
    file = 'corner_watchtowers.json'
    bone = (New-Bone -Name 'corner_watchtowers' -Pivot @(0, 32, 0) -Cubes $towerCubes.ToArray())
}

$entranceHallCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $entranceHallCubes -Cubes (New-HallShell -CenterX 0 -BaseY 40 -CenterZ -440 -Width 352 -Depth 240 -Height 256 -NorthDoorWidth 128 -NorthDoorHeight 160 -SouthDoorWidth 144 -SouthDoorHeight 176 -Uv @(0, 2360))
Add-Cubes -Target $entranceHallCubes -Cubes @(
    (New-Cube -Origin @(-120, 56, -520) -Size @(24, 176, 24) -Uv @(320, 2360)),
    (New-Cube -Origin @(-40, 56, -520) -Size @(24, 176, 24) -Uv @(350, 2360)),
    (New-Cube -Origin @(56, 56, -520) -Size @(24, 176, 24) -Uv @(380, 2360)),
    (New-Cube -Origin @(136, 56, -520) -Size @(24, 176, 24) -Uv @(410, 2360)),
    (New-Cube -Origin @(-176, 232, -560) -Size @(352, 16, 32) -Uv @(440, 2360)),
    (New-Cube -Origin @(-176, 232, -352) -Size @(352, 16, 32) -Uv @(440, 2400))
)
$parts += [ordered]@{
    file = 'entrance_hall.json'
    bone = (New-Bone -Name 'entrance_hall' -Pivot @(0, 40, -440) -Cubes $entranceHallCubes.ToArray())
}

$corridorCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $corridorCubes -Cubes (New-HallShell -CenterX -208 -BaseY 40 -CenterZ 120 -Width 96 -Depth 960 -Height 176 -NorthDoorWidth 80 -NorthDoorHeight 128 -SouthDoorWidth 80 -SouthDoorHeight 128 -EastDoorWidth 96 -EastDoorHeight 128 -WestDoorWidth 96 -WestDoorHeight 128 -Uv @(0, 2500))
Add-Cubes -Target $corridorCubes -Cubes (New-HallShell -CenterX 208 -BaseY 40 -CenterZ 120 -Width 96 -Depth 960 -Height 176 -NorthDoorWidth 80 -NorthDoorHeight 128 -SouthDoorWidth 80 -SouthDoorHeight 128 -EastDoorWidth 96 -EastDoorHeight 128 -WestDoorWidth 96 -WestDoorHeight 128 -Uv @(240, 2500))
$parts += [ordered]@{
    file = 'side_corridors.json'
    bone = (New-Bone -Name 'side_corridors' -Pivot @(0, 40, 120) -Cubes $corridorCubes.ToArray())
}

$leftWingCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $leftWingCubes -Cubes (New-HallShell -CenterX -400 -BaseY 40 -CenterZ 120 -Width 224 -Depth 736 -Height 224 -EastDoorWidth 128 -EastDoorHeight 160 -NorthDoorWidth 96 -NorthDoorHeight 144 -SouthDoorWidth 96 -SouthDoorHeight 144 -Uv @(0, 2680))
Add-Cubes -Target $leftWingCubes -Cubes @(
    (New-Cube -Origin @(-480, 56, -160) -Size @(16, 176, 560) -Uv @(260, 2680)),
    (New-Cube -Origin @(-320, 56, -160) -Size @(16, 176, 560) -Uv @(280, 2680))
)
$parts += [ordered]@{
    file = 'left_wing_shell.json'
    bone = (New-Bone -Name 'left_wing_shell' -Pivot @(-400, 40, 120) -Cubes $leftWingCubes.ToArray())
}

$rightWingCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $rightWingCubes -Cubes (New-HallShell -CenterX 400 -BaseY 40 -CenterZ 120 -Width 224 -Depth 736 -Height 224 -WestDoorWidth 128 -WestDoorHeight 160 -NorthDoorWidth 96 -NorthDoorHeight 144 -SouthDoorWidth 96 -SouthDoorHeight 144 -Uv @(0, 2860))
Add-Cubes -Target $rightWingCubes -Cubes @(
    (New-Cube -Origin @(304, 56, -160) -Size @(16, 176, 560) -Uv @(260, 2860)),
    (New-Cube -Origin @(464, 56, -160) -Size @(16, 176, 560) -Uv @(280, 2860))
)
$parts += [ordered]@{
    file = 'right_wing_shell.json'
    bone = (New-Bone -Name 'right_wing_shell' -Pivot @(400, 40, 120) -Cubes $rightWingCubes.ToArray())
}

$mainHallCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $mainHallCubes -Cubes (New-HallShell -CenterX 0 -BaseY 48 -CenterZ 104 -Width 480 -Depth 368 -Height 320 -NorthDoorWidth 160 -NorthDoorHeight 192 -SouthDoorWidth 160 -SouthDoorHeight 192 -WestDoorWidth 128 -WestDoorHeight 176 -EastDoorWidth 128 -EastDoorHeight 176 -Uv @(0, 3040))
Add-Cubes -Target $mainHallCubes -Cubes @(
    (New-Cube -Origin @(-160, 64, -16) -Size @(32, 240, 32) -Uv @(560, 3040)),
    (New-Cube -Origin @(-64, 64, -16) -Size @(32, 240, 32) -Uv @(600, 3040)),
    (New-Cube -Origin @(32, 64, -16) -Size @(32, 240, 32) -Uv @(640, 3040)),
    (New-Cube -Origin @(128, 64, -16) -Size @(32, 240, 32) -Uv @(680, 3040)),
    (New-Cube -Origin @(-224, 200, -72) -Size @(128, 16, 224) -Uv @(720, 3040)),
    (New-Cube -Origin @(96, 200, -72) -Size @(128, 16, 224) -Uv @(860, 3040)),
    (New-Cube -Origin @(-96, 48, 216) -Size @(192, 48, 80) -Uv @(1000, 3040)),
    (New-Cube -Origin @(-48, 96, 248) -Size @(96, 80, 48) -Uv @(1200, 3040))
)
$parts += [ordered]@{
    file = 'main_hall_shell.json'
    bone = (New-Bone -Name 'main_hall_shell' -Pivot @(0, 48, 104) -Cubes $mainHallCubes.ToArray())
}

$rearKeepCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $rearKeepCubes -Cubes (New-HallShell -CenterX 0 -BaseY 48 -CenterZ 576 -Width 352 -Depth 304 -Height 448 -NorthDoorWidth 144 -NorthDoorHeight 192 -SouthDoorWidth 128 -SouthDoorHeight 176 -Uv @(0, 3220))
Add-Cubes -Target $rearKeepCubes -Cubes @(
    (New-Cube -Origin @(-96, 272, 512) -Size @(192, 160, 176) -Uv @(420, 3220)),
    (New-Cube -Origin @(-32, 432, 576) -Size @(64, 96, 64) -Uv @(620, 3220)),
    (New-Cube -Origin @(-144, 96, 688) -Size @(288, 24, 32) -Uv @(700, 3220)),
    (New-Cube -Origin @(-176, 432, 512) -Size @(352, 16, 304) -Uv @(1000, 3220))
)
$parts += [ordered]@{
    file = 'rear_keep.json'
    bone = (New-Bone -Name 'rear_keep' -Pivot @(0, 48, 576) -Cubes $rearKeepCubes.ToArray())
}

$interiorCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $interiorCubes -Cubes @(
    (New-Cube -Origin @(-496, 120, -224) -Size @(192, 12, 608) -Uv @(0, 3400)),
    (New-Cube -Origin @(304, 120, -224) -Size @(192, 12, 608) -Uv @(220, 3400)),
    (New-Cube -Origin @(-464, 56, -64) -Size @(128, 160, 16) -Uv @(440, 3400)),
    (New-Cube -Origin @(-464, 56, 160) -Size @(128, 160, 16) -Uv @(580, 3400)),
    (New-Cube -Origin @(336, 56, -64) -Size @(128, 160, 16) -Uv @(720, 3400)),
    (New-Cube -Origin @(336, 56, 160) -Size @(128, 160, 16) -Uv @(860, 3400)),
    (New-Cube -Origin @(-80, 144, -56) -Size @(160, 16, 192) -Uv @(1000, 3400)),
    (New-Cube -Origin @(-96, 120, 616) -Size @(192, 12, 96) -Uv @(1180, 3400)),
    (New-Cube -Origin @(-112, 176, 536) -Size @(16, 176, 224) -Uv @(1380, 3400)),
    (New-Cube -Origin @(96, 176, 536) -Size @(16, 176, 224) -Uv @(1400, 3400))
)
$parts += [ordered]@{
    file = 'interior_layout.json'
    bone = (New-Bone -Name 'interior_layout' -Pivot @(0, 56, 120) -Cubes $interiorCubes.ToArray())
}

$roofCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $roofCubes -Cubes (New-RoofTier -CenterX 0 -CenterZ -440 -BaseY 280 -Width 320 -Depth 208 -Uv @(0, 3560))
Add-Cubes -Target $roofCubes -Cubes (New-RoofTier -CenterX 0 -CenterZ 104 -BaseY 360 -Width 448 -Depth 320 -Uv @(220, 3560))
Add-Cubes -Target $roofCubes -Cubes (New-RoofTier -CenterX -400 -CenterZ 120 -BaseY 248 -Width 192 -Depth 704 -Uv @(520, 3560))
Add-Cubes -Target $roofCubes -Cubes (New-RoofTier -CenterX 400 -CenterZ 120 -BaseY 248 -Width 192 -Depth 704 -Uv @(820, 3560))
Add-Cubes -Target $roofCubes -Cubes (New-RoofTier -CenterX 0 -CenterZ 576 -BaseY 448 -Width 320 -Depth 272 -Uv @(1120, 3560))
Add-Cubes -Target $roofCubes -Cubes (New-RoofTier -CenterX 0 -CenterZ -760 -BaseY 384 -Width 304 -Depth 112 -Uv @(1420, 3560))
$parts += [ordered]@{
    file = 'grand_roof.json'
    bone = (New-Bone -Name 'grand_roof' -Pivot @(0, 280, 104) -Cubes $roofCubes.ToArray())
}

$accentCubes = [System.Collections.ArrayList]::new()
Add-Cubes -Target $accentCubes -Cubes @(
    (New-Cube -Origin @(-8, 426, -24) -Size @(16, 96, 16) -Uv @(0, 3880)),
    (New-Cube -Origin @(-8, 510, -8) -Size @(16, 80, 16) -Uv @(20, 3880)),
    (New-Cube -Origin @(-8, 514, 568) -Size @(16, 140, 16) -Uv @(40, 3880)),
    (New-Cube -Origin @(-8, 646, 576) -Size @(16, 80, 16) -Uv @(60, 3880)),
    (New-Cube -Origin @(-560, 408, -784) -Size @(32, 72, 32) -Uv @(80, 3880)),
    (New-Cube -Origin @(528, 408, -784) -Size @(32, 72, 32) -Uv @(120, 3880)),
    (New-Cube -Origin @(-560, 408, 720) -Size @(32, 72, 32) -Uv @(160, 3880)),
    (New-Cube -Origin @(528, 408, 720) -Size @(32, 72, 32) -Uv @(200, 3880)),
    (New-Cube -Origin @(-24, 392, -504) -Size @(48, 18, 18) -Uv @(240, 3880)),
    (New-Cube -Origin @(-24, 472, 520) -Size @(48, 18, 18) -Uv @(300, 3880))
)
$parts += [ordered]@{
    file = 'roof_accents.json'
    bone = (New-Bone -Name 'roof_accents' -Pivot @(0, 400, 120) -Cubes $accentCubes.ToArray())
}

$partFiles = [System.Collections.ArrayList]::new()
$fullBones = [System.Collections.ArrayList]::new()
[void]$fullBones.Add([ordered]@{
    name = 'root'
    pivot = @(0, 0, 0)
})

foreach ($part in $parts) {
    $path = Join-Path $PartsDir $part.file
    $identifier = 'geometry.chen_mod.new_palace.' + [System.IO.Path]::GetFileNameWithoutExtension($part.file)
    $partFile = New-PartFile -Identifier $identifier -Bones @(
        [ordered]@{
            name = 'root'
            pivot = @(0, 0, 0)
        },
        $part.bone
    )
    Write-JsonFile -Path $path -Data $partFile
    [void]$partFiles.Add($path)
    [void]$fullBones.Add($part.bone)
}

$fullModel = [ordered]@{
    format_version = '1.16.0'
    'minecraft:geometry' = @(
        [ordered]@{
            description = [ordered]@{
                identifier = 'geometry.chen_mod.new_palace.full'
                texture_width = $TextureWidth
                texture_height = $TextureHeight
            }
            bones = $fullBones
        }
    )
}

Write-JsonFile -Path $FullModelPath -Data $fullModel

Write-Host "Generated new palace kit in $PartsDir"
Write-Host "Part files: $($partFiles.Count)"
Write-Host "Full model: $FullModelPath"
