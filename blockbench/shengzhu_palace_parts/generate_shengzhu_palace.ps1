$ErrorActionPreference = 'Stop'
$TextureWidth = 2048
$TextureHeight = 4096

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
        size   = $Size
        uv     = $Uv
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
        [string]$Parent = $null,
        [object[]]$Pivot,
        [object[]]$Cubes = @()
    )

    $bone = [ordered]@{
        name  = $Name
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
        format_version       = '1.16.0'
        'minecraft:geometry' = @(
            [ordered]@{
                description = [ordered]@{
                    identifier     = $Identifier
                    texture_width  = $TextureWidth
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
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    $json = $Data | ConvertTo-Json -Depth 100
    [System.IO.File]::WriteAllText($Path, $json)
}

function Offset-BoneUv {
    param(
        $Bone,
        [int]$OffsetX,
        [int]$OffsetY
    )

    if (-not $Bone.Contains('cubes')) {
        return
    }

    foreach ($cube in $Bone.cubes) {
        if ($cube.Contains('uv') -and $cube.uv -is [System.Array] -and $cube.uv.Count -ge 2) {
            $u = [int]$cube.uv[0]
            $v = [int]$cube.uv[1]
            $cube.uv = @(($u + $OffsetX), ($v + $OffsetY))
        }
    }
}

function New-RoofCorners {
    param(
        [double]$CenterX,
        [double]$CenterZ,
        [double]$Y,
        [double]$Width,
        [double]$Depth,
        [double]$SizeX = 12,
        [double]$SizeZ = 7,
        [double]$RotationZ = 24,
        [object[]]$Uv = @(0, 0)
    )

    $halfWidth = $Width / 2
    $halfDepth = $Depth / 2
    $uvX = [int]$Uv[0]
    $uvY = [int]$Uv[1]
    $cubes = [System.Collections.ArrayList]::new()

    [void]$cubes.Add((New-Cube -Origin @((($CenterX - $halfWidth - $SizeX + 4)), $Y, (($CenterZ - $halfDepth - $SizeZ + 4))) -Size @($SizeX, 1, $SizeZ) -Uv @($uvX, $uvY) -Pivot @((($CenterX - $halfWidth + 4)), (($Y + 0.5)), (($CenterZ - $halfDepth + 4))) -Rotation @(0, 0, $RotationZ)))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX + $halfWidth - 4)), $Y, (($CenterZ - $halfDepth - $SizeZ + 4))) -Size @($SizeX, 1, $SizeZ) -Uv @((($uvX + 14)), $uvY) -Pivot @((($CenterX + $halfWidth - 4)), (($Y + 0.5)), (($CenterZ - $halfDepth + 4))) -Rotation @(0, 0, -$RotationZ)))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - $halfWidth - $SizeX + 4)), $Y, (($CenterZ + $halfDepth - 4))) -Size @($SizeX, 1, $SizeZ) -Uv @($uvX, (($uvY + 8))) -Pivot @((($CenterX - $halfWidth + 4)), (($Y + 0.5)), (($CenterZ + $halfDepth - 4))) -Rotation @(0, 0, -$RotationZ)))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX + $halfWidth - 4)), $Y, (($CenterZ + $halfDepth - 4))) -Size @($SizeX, 1, $SizeZ) -Uv @((($uvX + 14)), (($uvY + 8))) -Pivot @((($CenterX + $halfWidth - 4)), (($Y + 0.5)), (($CenterZ + $halfDepth - 4))) -Rotation @(0, 0, $RotationZ)))

    return $cubes.ToArray()
}

function New-PagodaCubes {
    param(
        [double]$CenterX,
        [double]$BaseY,
        [double]$CenterZ,
        [double]$BaseWidth,
        [double]$BaseHeight,
        [double]$Roof1Width,
        [double]$MidWidth,
        [double]$MidHeight,
        [double]$Roof2Width,
        [double]$UpperWidth,
        [double]$UpperHeight,
        [double]$Roof3Width,
        [double]$CapWidth,
        [double]$CapHeight,
        [double]$SpireHeight,
        [int]$UvX,
        [int]$UvY
    )

    $roof1Y = $BaseY + $BaseHeight - 1
    $midY = $roof1Y + 5
    $roof2Y = $midY + $MidHeight - 1
    $upperY = $roof2Y + 5
    $roof3Y = $upperY + $UpperHeight - 1
    $capY = $roof3Y + 4
    $spireY = $capY + $CapHeight

    $cubes = [System.Collections.ArrayList]::new()

    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($BaseWidth / 2))), $BaseY, (($CenterZ - ($BaseWidth / 2)))) -Size @($BaseWidth, $BaseHeight, $BaseWidth) -Uv @($UvX, $UvY)))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($Roof1Width / 2))), $roof1Y, (($CenterZ - ($Roof1Width / 2)))) -Size @($Roof1Width, 4, $Roof1Width) -Uv @((($UvX + 40)), $UvY)))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - (($Roof1Width - 6) / 2))), (($roof1Y + 4)), (($CenterZ - (($Roof1Width - 6) / 2)))) -Size @((($Roof1Width - 6)), 2, (($Roof1Width - 6))) -Uv @((($UvX + 40)), (($UvY + 40)))))
    foreach ($cube in (New-RoofCorners -CenterX $CenterX -CenterZ $CenterZ -Y ($roof1Y + 0.5) -Width $Roof1Width -Depth $Roof1Width -SizeX 8 -SizeZ 6 -RotationZ 24 -Uv @((($UvX + 108)), $UvY))) { [void]$cubes.Add($cube) }
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($MidWidth / 2))), $midY, (($CenterZ - ($MidWidth / 2)))) -Size @($MidWidth, $MidHeight, $MidWidth) -Uv @($UvX, (($UvY + 54)))))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($Roof2Width / 2))), $roof2Y, (($CenterZ - ($Roof2Width / 2)))) -Size @($Roof2Width, 4, $Roof2Width) -Uv @((($UvX + 40)), (($UvY + 54)))))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - (($Roof2Width - 4) / 2))), (($roof2Y + 4)), (($CenterZ - (($Roof2Width - 4) / 2)))) -Size @((($Roof2Width - 4)), 2, (($Roof2Width - 4))) -Uv @((($UvX + 40)), (($UvY + 84)))))
    foreach ($cube in (New-RoofCorners -CenterX $CenterX -CenterZ $CenterZ -Y ($roof2Y + 0.5) -Width $Roof2Width -Depth $Roof2Width -SizeX 7 -SizeZ 5 -RotationZ 20 -Uv @((($UvX + 108)), (($UvY + 18))))) { [void]$cubes.Add($cube) }
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($UpperWidth / 2))), $upperY, (($CenterZ - ($UpperWidth / 2)))) -Size @($UpperWidth, $UpperHeight, $UpperWidth) -Uv @($UvX, (($UvY + 96)))))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($Roof3Width / 2))), $roof3Y, (($CenterZ - ($Roof3Width / 2)))) -Size @($Roof3Width, 3, $Roof3Width) -Uv @((($UvX + 40)), (($UvY + 96)))))
    foreach ($cube in (New-RoofCorners -CenterX $CenterX -CenterZ $CenterZ -Y ($roof3Y + 0.5) -Width $Roof3Width -Depth $Roof3Width -SizeX 5 -SizeZ 4 -RotationZ 18 -Uv @((($UvX + 108)), (($UvY + 34))))) { [void]$cubes.Add($cube) }
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - ($CapWidth / 2))), $capY, (($CenterZ - ($CapWidth / 2)))) -Size @($CapWidth, $CapHeight, $CapWidth) -Uv @($UvX, (($UvY + 128)))))
    [void]$cubes.Add((New-Cube -Origin @((($CenterX - 1)), $spireY, (($CenterZ - 1))) -Size @(2, $SpireHeight, 2) -Uv @((($UvX + 24)), (($UvY + 128)))))

    return $cubes.ToArray()
}

$partsDir = Join-Path $PSScriptRoot '.'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$fullModelPath = Join-Path $partsDir 'shengzhu_palace_full.geo.json'
$assetModelPath = Join-Path $projectRoot 'src/main/resources/assets/chen_mod/geo/shengzhu_palace.geo.json'

$parts = @()

$parts += [ordered]@{
    file = 'foundation_base.json'
    bone = New-Bone -Name 'foundation_base' -Parent 'root' -Pivot @(0, 0, 0) -Cubes @(
        New-Cube -Origin @(-92, 0, -112) -Size @(184, 12, 200) -Uv @(0, 0)
        New-Cube -Origin @(-124, 0, -88) -Size @(32, 12, 152) -Uv @(0, 216)
        New-Cube -Origin @(92, 0, -88) -Size @(32, 12, 152) -Uv @(64, 216)
        New-Cube -Origin @(-56, 0, -144) -Size @(112, 12, 32) -Uv @(104, 216)
        New-Cube -Origin @(-72, 0, 88) -Size @(144, 12, 28) -Uv @(220, 216)
        New-Cube -Origin @(-106, -6, -126) -Size @(212, 6, 232) -Uv @(0, 268)
        New-Cube -Origin @(-136, -6, -98) -Size @(30, 6, 176) -Uv @(218, 268)
        New-Cube -Origin @(106, -6, -98) -Size @(30, 6, 176) -Uv @(260, 268)
        New-Cube -Origin @(-98, 12, -118) -Size @(196, 3, 212) -Uv @(0, 452)
        New-Cube -Origin @(-36, 12, -150) -Size @(72, 4, 38) -Uv @(202, 452)
    )
}

$parts += [ordered]@{
    file = 'courtyard_floor.json'
    bone = New-Bone -Name 'courtyard_floor' -Parent 'root' -Pivot @(0, 15, 0) -Cubes @(
        New-Cube -Origin @(-92, 15, -112) -Size @(184, 5, 190) -Uv @(0, 0)
        New-Cube -Origin @(-26, 15, -144) -Size @(52, 5, 32) -Uv @(188, 0)
        New-Cube -Origin @(-108, 15, -84) -Size @(16, 5, 132) -Uv @(246, 0)
        New-Cube -Origin @(92, 15, -84) -Size @(16, 5, 132) -Uv @(268, 0)
        New-Cube -Origin @(-76, 15, 78) -Size @(152, 5, 18) -Uv @(290, 0)
        New-Cube -Origin @(-40, 20, -32) -Size @(80, 6, 46) -Uv @(0, 202)
    )
}

$parts += [ordered]@{
    file = 'outer_ring_walls.json'
    bone = New-Bone -Name 'outer_ring_walls' -Parent 'root' -Pivot @(0, 20, 0) -Cubes @(
        New-Cube -Origin @(-100, 20, -122) -Size @(56, 30, 10) -Uv @(0, 0)
        New-Cube -Origin @(44, 20, -122) -Size @(56, 30, 10) -Uv @(72, 0)
        New-Cube -Origin @(-34, 20, -124) -Size @(10, 34, 14) -Uv @(144, 0)
        New-Cube -Origin @(24, 20, -124) -Size @(10, 34, 14) -Uv @(160, 0)
        New-Cube -Origin @(-24, 46, -124) -Size @(48, 8, 14) -Uv @(176, 0)
        New-Cube -Origin @(-128, 20, -90) -Size @(16, 30, 162) -Uv @(0, 48)
        New-Cube -Origin @(112, 20, -90) -Size @(16, 30, 162) -Uv @(24, 48)
        New-Cube -Origin @(-96, 20, 88) -Size @(192, 30, 14) -Uv @(48, 48)
        New-Cube -Origin @(-116, 20, -112) -Size @(20, 30, 12) -Uv @(0, 220) -Pivot @(-106, 20, -106) -Rotation @(0, 26, 0)
        New-Cube -Origin @(96, 20, -112) -Size @(20, 30, 12) -Uv @(36, 220) -Pivot @(106, 20, -106) -Rotation @(0, -26, 0)
        New-Cube -Origin @(-116, 20, 76) -Size @(20, 30, 12) -Uv @(72, 220) -Pivot @(-106, 20, 82) -Rotation @(0, -26, 0)
        New-Cube -Origin @(96, 20, 76) -Size @(20, 30, 12) -Uv @(108, 220) -Pivot @(106, 20, 82) -Rotation @(0, 26, 0)
        New-Cube -Origin @(-100, 50, -124) -Size @(56, 4, 14) -Uv @(0, 256)
        New-Cube -Origin @(44, 50, -124) -Size @(56, 4, 14) -Uv @(72, 256)
        New-Cube -Origin @(-132, 50, -92) -Size @(20, 4, 166) -Uv @(0, 276)
        New-Cube -Origin @(112, 50, -92) -Size @(20, 4, 166) -Uv @(24, 276)
        New-Cube -Origin @(-98, 50, 88) -Size @(196, 4, 18) -Uv @(52, 276)
    )
}

$parts += [ordered]@{
    file = 'front_gate.json'
    bone = New-Bone -Name 'front_gate' -Parent 'root' -Pivot @(0, 20, -132) -Cubes @(
        New-Cube -Origin @(-24, 20, -138) -Size @(8, 26, 16) -Uv @(0, 0)
        New-Cube -Origin @(16, 20, -138) -Size @(8, 26, 16) -Uv @(24, 0)
        New-Cube -Origin @(-16, 44, -138) -Size @(32, 8, 16) -Uv @(48, 0)
        New-Cube -Origin @(-28, 52, -142) -Size @(56, 4, 24) -Uv @(88, 0)
        New-Cube -Origin @(-24, 56, -138) -Size @(48, 3, 16) -Uv @(148, 0)
        New-Cube -Origin @(-18, 52, -134) -Size @(36, 16, 12) -Uv @(0, 46)
        New-Cube -Origin @(-24, 68, -138) -Size @(48, 3, 20) -Uv @(44, 46)
        New-Cube -Origin @(-18, 71, -132) -Size @(36, 8, 12) -Uv @(98, 46)
        New-Cube -Origin @(-1, 79, -127) -Size @(2, 14, 2) -Uv @(140, 46)
        New-RoofCorners -CenterX 0 -CenterZ -130 -Y 53 -Width 56 -Depth 24 -SizeX 12 -SizeZ 6 -RotationZ 24 -Uv @(156, 0)
        New-RoofCorners -CenterX 0 -CenterZ -128 -Y 68.5 -Width 48 -Depth 20 -SizeX 10 -SizeZ 5 -RotationZ 20 -Uv @(156, 22)
    )
}

$parts += [ordered]@{
    file = 'front_stairway.json'
    bone = New-Bone -Name 'front_stairway' -Parent 'root' -Pivot @(0, 0, -140) -Cubes @(
        New-Cube -Origin @(-44, 0, -160) -Size @(88, 4, 20) -Uv @(0, 0)
        New-Cube -Origin @(-36, 4, -148) -Size @(72, 4, 16) -Uv @(92, 0)
        New-Cube -Origin @(-30, 8, -136) -Size @(60, 4, 12) -Uv @(168, 0)
        New-Cube -Origin @(-24, 12, -126) -Size @(48, 4, 10) -Uv @(0, 28)
        New-Cube -Origin @(-28, 16, -122) -Size @(56, 4, 16) -Uv @(52, 28)
        New-Cube -Origin @(-56, 0, -154) -Size @(12, 20, 30) -Uv @(114, 28)
        New-Cube -Origin @(44, 0, -154) -Size @(12, 20, 30) -Uv @(132, 28)
        New-Cube -Origin @(-16, 0, -148) -Size @(32, 18, 8) -Uv @(150, 28)
    )
}

$parts += [ordered]@{
    file = 'corner_watchtowers.json'
    bone = New-Bone -Name 'corner_watchtowers' -Parent 'root' -Pivot @(0, 50, 0) -Cubes @(
        New-Cube -Origin @(-112, 50, -118) -Size @(12, 16, 12) -Uv @(0, 0)
        New-Cube -Origin @(-116, 66, -122) -Size @(20, 4, 20) -Uv @(18, 0)
        New-Cube -Origin @(-110, 70, -116) -Size @(8, 10, 8) -Uv @(42, 0)
        New-Cube -Origin @(-107, 80, -113) -Size @(2, 12, 2) -Uv @(54, 0)
        New-RoofCorners -CenterX -106 -CenterZ -112 -Y 66.5 -Width 20 -Depth 20 -SizeX 6 -SizeZ 4 -RotationZ 20 -Uv @(60, 0)
        New-Cube -Origin @(100, 50, -118) -Size @(12, 16, 12) -Uv @(0, 20)
        New-Cube -Origin @(96, 66, -122) -Size @(20, 4, 20) -Uv @(18, 20)
        New-Cube -Origin @(102, 70, -116) -Size @(8, 10, 8) -Uv @(42, 20)
        New-Cube -Origin @(105, 80, -113) -Size @(2, 12, 2) -Uv @(54, 20)
        New-RoofCorners -CenterX 106 -CenterZ -112 -Y 66.5 -Width 20 -Depth 20 -SizeX 6 -SizeZ 4 -RotationZ 20 -Uv @(60, 20)
        New-Cube -Origin @(-112, 50, 82) -Size @(12, 16, 12) -Uv @(0, 40)
        New-Cube -Origin @(-116, 66, 78) -Size @(20, 4, 20) -Uv @(18, 40)
        New-Cube -Origin @(-110, 70, 84) -Size @(8, 10, 8) -Uv @(42, 40)
        New-Cube -Origin @(-107, 80, 87) -Size @(2, 12, 2) -Uv @(54, 40)
        New-RoofCorners -CenterX -106 -CenterZ 88 -Y 66.5 -Width 20 -Depth 20 -SizeX 6 -SizeZ 4 -RotationZ 20 -Uv @(60, 40)
        New-Cube -Origin @(100, 50, 82) -Size @(12, 16, 12) -Uv @(0, 60)
        New-Cube -Origin @(96, 66, 78) -Size @(20, 4, 20) -Uv @(18, 60)
        New-Cube -Origin @(102, 70, 84) -Size @(8, 10, 8) -Uv @(42, 60)
        New-Cube -Origin @(105, 80, 87) -Size @(2, 12, 2) -Uv @(54, 60)
        New-RoofCorners -CenterX 106 -CenterZ 88 -Y 66.5 -Width 20 -Depth 20 -SizeX 6 -SizeZ 4 -RotationZ 20 -Uv @(60, 60)
    )
}

$parts += [ordered]@{
    file = 'left_wing_shell.json'
    bone = New-Bone -Name 'left_wing_shell' -Parent 'root' -Pivot @(-76, 42, 8) -Cubes @(
        New-Cube -Origin @(-92, 42, -28) -Size @(36, 34, 8) -Uv @(0, 0)
        New-Cube -Origin @(-92, 42, 32) -Size @(36, 34, 8) -Uv @(48, 0)
        New-Cube -Origin @(-96, 42, -28) -Size @(8, 34, 68) -Uv @(96, 0)
        New-Cube -Origin @(-60, 42, -10) -Size @(8, 34, 50) -Uv @(110, 0)
        New-Cube -Origin @(-88, 76, -20) -Size @(28, 18, 52) -Uv @(124, 0)
        New-Cube -Origin @(-82, 94, -4) -Size @(20, 18, 20) -Uv @(182, 0)
        New-Cube -Origin @(-100, 42, -56) -Size @(48, 24, 24) -Uv @(206, 0)
        New-Cube -Origin @(-108, 54, -16) -Size @(12, 28, 40) -Uv @(0, 84)
    )
}

$parts += [ordered]@{
    file = 'right_wing_shell.json'
    bone = New-Bone -Name 'right_wing_shell' -Parent 'root' -Pivot @(76, 42, 8) -Cubes @(
        New-Cube -Origin @(56, 42, -28) -Size @(36, 34, 8) -Uv @(0, 0)
        New-Cube -Origin @(56, 42, 32) -Size @(36, 34, 8) -Uv @(48, 0)
        New-Cube -Origin @(88, 42, -28) -Size @(8, 34, 68) -Uv @(96, 0)
        New-Cube -Origin @(52, 42, -10) -Size @(8, 34, 50) -Uv @(110, 0)
        New-Cube -Origin @(60, 76, -20) -Size @(28, 18, 52) -Uv @(124, 0)
        New-Cube -Origin @(62, 94, -4) -Size @(20, 18, 20) -Uv @(182, 0)
        New-Cube -Origin @(52, 42, -56) -Size @(48, 24, 24) -Uv @(206, 0)
        New-Cube -Origin @(96, 54, -16) -Size @(12, 28, 40) -Uv @(0, 84)
    )
}

$parts += [ordered]@{
    file = 'side_buttresses.json'
    bone = New-Bone -Name 'side_buttresses' -Parent 'root' -Pivot @(0, 20, -28) -Cubes @(
        New-Cube -Origin @(-60, 20, -40) -Size @(18, 26, 30) -Uv @(0, 0) -Pivot @(-51, 20, -25) -Rotation @(-14, 0, 0)
        New-Cube -Origin @(-64, 18, -44) -Size @(26, 4, 38) -Uv @(54, 0)
        New-Cube -Origin @(42, 20, -40) -Size @(18, 26, 30) -Uv @(88, 0) -Pivot @(51, 20, -25) -Rotation @(-14, 0, 0)
        New-Cube -Origin @(38, 18, -44) -Size @(26, 4, 38) -Uv @(142, 0)
        New-Cube -Origin @(-26, 20, -48) -Size @(52, 4, 22) -Uv @(176, 0)
        New-Cube -Origin @(-22, 24, -42) -Size @(44, 4, 16) -Uv @(0, 44)
        New-Cube -Origin @(-18, 28, -36) -Size @(36, 4, 10) -Uv @(50, 44)
        New-Cube -Origin @(-12, 32, -28) -Size @(24, 4, 8) -Uv @(92, 44)
    )
}

$parts += [ordered]@{
    file = 'main_hall_shell.json'
    bone = New-Bone -Name 'main_hall_shell' -Parent 'root' -Pivot @(0, 42, 6) -Cubes @(
        New-Cube -Origin @(-44, 42, -18) -Size @(16, 48, 8) -Uv @(0, 0)
        New-Cube -Origin @(28, 42, -18) -Size @(16, 48, 8) -Uv @(24, 0)
        New-Cube -Origin @(-28, 70, -18) -Size @(56, 20, 8) -Uv @(48, 0)
        New-Cube -Origin @(-52, 42, -18) -Size @(8, 48, 56) -Uv @(108, 0)
        New-Cube -Origin @(44, 42, -18) -Size @(8, 48, 56) -Uv @(122, 0)
        New-Cube -Origin @(-44, 42, 30) -Size @(88, 48, 8) -Uv @(136, 0)
        New-Cube -Origin @(-22, 42, 8) -Size @(44, 18, 18) -Uv @(0, 64)
        New-Cube -Origin @(-18, 60, 2) -Size @(36, 10, 24) -Uv @(48, 64)
        New-Cube -Origin @(-14, 90, -6) -Size @(28, 18, 32) -Uv @(92, 64)
    )
}

$parts += [ordered]@{
    file = 'rear_keep.json'
    bone = New-Bone -Name 'rear_keep' -Parent 'root' -Pivot @(0, 54, 34) -Cubes @(
        New-Cube -Origin @(-70, 54, 10) -Size @(30, 50, 52) -Uv @(0, 0)
        New-Cube -Origin @(40, 54, 10) -Size @(30, 50, 52) -Uv @(34, 0)
        New-Cube -Origin @(-28, 58, 18) -Size @(56, 42, 46) -Uv @(68, 0)
        New-Cube -Origin @(-48, 50, 0) -Size @(96, 8, 18) -Uv @(128, 0)
        New-Cube -Origin @(-60, 104, 18) -Size @(120, 6, 48) -Uv @(0, 58)
        New-Cube -Origin @(-88, 80, 26) -Size @(18, 30, 26) -Uv @(124, 58)
        New-Cube -Origin @(70, 80, 26) -Size @(18, 30, 26) -Uv @(146, 58)
    )
}

$parts += [ordered]@{
    file = 'grand_roof.json'
    bone = New-Bone -Name 'grand_roof' -Parent 'root' -Pivot @(0, 86, 6) -Cubes @(
        New-Cube -Origin @(-112, 86, -52) -Size @(224, 10, 40) -Uv @(0, 0)
        New-Cube -Origin @(-120, 80, -58) -Size @(240, 6, 52) -Uv @(0, 46)
        New-Cube -Origin @(-100, 84, -12) -Size @(56, 8, 78) -Uv @(0, 106)
        New-Cube -Origin @(44, 84, -12) -Size @(56, 8, 78) -Uv @(60, 106)
        New-Cube -Origin @(-88, 88, 40) -Size @(176, 8, 28) -Uv @(120, 106)
        New-RoofCorners -CenterX 0 -CenterZ -32 -Y 81 -Width 240 -Depth 52 -SizeX 18 -SizeZ 10 -RotationZ 24 -Uv @(0, 144)
        New-RoofCorners -CenterX -72 -CenterZ 27 -Y 84.5 -Width 56 -Depth 78 -SizeX 12 -SizeZ 8 -RotationZ 22 -Uv @(68, 144)
        New-RoofCorners -CenterX 72 -CenterZ 27 -Y 84.5 -Width 56 -Depth 78 -SizeX 12 -SizeZ 8 -RotationZ 22 -Uv @(120, 144)
    )
}

$parts += [ordered]@{
    file = 'upper_roof.json'
    bone = New-Bone -Name 'upper_roof' -Parent 'root' -Pivot @(0, 108, 0) -Cubes @(
        New-Cube -Origin @(-58, 102, -30) -Size @(116, 8, 56) -Uv @(0, 0)
        New-Cube -Origin @(-68, 96, -36) -Size @(136, 6, 68) -Uv @(0, 64)
        New-Cube -Origin @(-34, 118, -18) -Size @(68, 7, 34) -Uv @(140, 0)
        New-Cube -Origin @(-42, 114, -24) -Size @(84, 5, 46) -Uv @(140, 42)
        New-Cube -Origin @(-18, 125, -8) -Size @(36, 6, 16) -Uv @(230, 42)
        New-RoofCorners -CenterX 0 -CenterZ 0 -Y 97 -Width 136 -Depth 68 -SizeX 16 -SizeZ 9 -RotationZ 22 -Uv @(0, 118)
        New-RoofCorners -CenterX 0 -CenterZ -1 -Y 114.5 -Width 84 -Depth 46 -SizeX 12 -SizeZ 7 -RotationZ 20 -Uv @(72, 118)
    )
}

$parts += [ordered]@{
    file = 'central_tower.json'
    bone = New-Bone -Name 'central_tower' -Parent 'root' -Pivot @(0, 126, 0) -Cubes @(
        New-Cube -Origin @(-24, 126, -18) -Size @(48, 36, 36) -Uv @(0, 0)
        New-Cube -Origin @(-36, 160, -28) -Size @(72, 7, 56) -Uv @(52, 0)
        New-Cube -Origin @(-42, 156, -34) -Size @(84, 5, 68) -Uv @(52, 62)
        New-RoofCorners -CenterX 0 -CenterZ 0 -Y 160.5 -Width 84 -Depth 68 -SizeX 14 -SizeZ 10 -RotationZ 22 -Uv @(144, 0)
        New-Cube -Origin @(-16, 167, -10) -Size @(32, 28, 20) -Uv @(0, 80)
        New-Cube -Origin @(-28, 193, -18) -Size @(56, 6, 36) -Uv @(36, 80)
        New-Cube -Origin @(-34, 189, -24) -Size @(68, 4, 48) -Uv @(36, 122)
        New-RoofCorners -CenterX 0 -CenterZ 0 -Y 193.5 -Width 68 -Depth 48 -SizeX 12 -SizeZ 8 -RotationZ 20 -Uv @(118, 80)
        New-Cube -Origin @(-10, 199, -6) -Size @(20, 22, 12) -Uv @(0, 118)
        New-Cube -Origin @(-18, 219, -14) -Size @(36, 5, 28) -Uv @(36, 172)
        New-RoofCorners -CenterX 0 -CenterZ 0 -Y 219.5 -Width 36 -Depth 28 -SizeX 8 -SizeZ 5 -RotationZ 18 -Uv @(82, 172)
        New-Cube -Origin @(-6, 224, -4) -Size @(12, 16, 8) -Uv @(0, 146)
        New-Cube -Origin @(-10, 240, -8) -Size @(20, 4, 16) -Uv @(18, 146)
        New-Cube -Origin @(-2, 244, -1) -Size @(4, 32, 2) -Uv @(42, 146)
    )
}

$parts += [ordered]@{
    file = 'left_front_pagoda.json'
    bone = New-Bone -Name 'left_front_pagoda' -Parent 'root' -Pivot @(-74, 88, -70) -Cubes @(
        New-PagodaCubes -CenterX -74 -BaseY 88 -CenterZ -70 -BaseWidth 18 -BaseHeight 24 -Roof1Width 34 -MidWidth 14 -MidHeight 16 -Roof2Width 26 -UpperWidth 10 -UpperHeight 14 -Roof3Width 18 -CapWidth 6 -CapHeight 12 -SpireHeight 18 -UvX 0 -UvY 0
    )
}

$parts += [ordered]@{
    file = 'right_front_pagoda.json'
    bone = New-Bone -Name 'right_front_pagoda' -Parent 'root' -Pivot @(74, 88, -70) -Cubes @(
        New-PagodaCubes -CenterX 74 -BaseY 88 -CenterZ -70 -BaseWidth 18 -BaseHeight 24 -Roof1Width 34 -MidWidth 14 -MidHeight 16 -Roof2Width 26 -UpperWidth 10 -UpperHeight 14 -Roof3Width 18 -CapWidth 6 -CapHeight 12 -SpireHeight 18 -UvX 152 -UvY 0
    )
}

$parts += [ordered]@{
    file = 'left_mid_pagoda.json'
    bone = New-Bone -Name 'left_mid_pagoda' -Parent 'root' -Pivot @(-46, 104, -8) -Cubes @(
        New-PagodaCubes -CenterX -46 -BaseY 104 -CenterZ -8 -BaseWidth 14 -BaseHeight 18 -Roof1Width 26 -MidWidth 12 -MidHeight 14 -Roof2Width 20 -UpperWidth 8 -UpperHeight 12 -Roof3Width 14 -CapWidth 5 -CapHeight 10 -SpireHeight 16 -UvX 0 -UvY 164
    )
}

$parts += [ordered]@{
    file = 'right_mid_pagoda.json'
    bone = New-Bone -Name 'right_mid_pagoda' -Parent 'root' -Pivot @(46, 104, -8) -Cubes @(
        New-PagodaCubes -CenterX 46 -BaseY 104 -CenterZ -8 -BaseWidth 14 -BaseHeight 18 -Roof1Width 26 -MidWidth 12 -MidHeight 14 -Roof2Width 20 -UpperWidth 8 -UpperHeight 12 -Roof3Width 14 -CapWidth 5 -CapHeight 10 -SpireHeight 16 -UvX 152 -UvY 164
    )
}

$parts += [ordered]@{
    file = 'left_rear_pagoda.json'
    bone = New-Bone -Name 'left_rear_pagoda' -Parent 'root' -Pivot @(-88, 104, 38) -Cubes @(
        New-PagodaCubes -CenterX -88 -BaseY 104 -CenterZ 38 -BaseWidth 16 -BaseHeight 22 -Roof1Width 30 -MidWidth 12 -MidHeight 16 -Roof2Width 24 -UpperWidth 9 -UpperHeight 12 -Roof3Width 16 -CapWidth 5 -CapHeight 12 -SpireHeight 18 -UvX 304 -UvY 0
    )
}

$parts += [ordered]@{
    file = 'right_rear_pagoda.json'
    bone = New-Bone -Name 'right_rear_pagoda' -Parent 'root' -Pivot @(88, 104, 38) -Cubes @(
        New-PagodaCubes -CenterX 88 -BaseY 104 -CenterZ 38 -BaseWidth 16 -BaseHeight 22 -Roof1Width 30 -MidWidth 12 -MidHeight 16 -Roof2Width 24 -UpperWidth 9 -UpperHeight 12 -Roof3Width 16 -CapWidth 5 -CapHeight 12 -SpireHeight 18 -UvX 304 -UvY 164
    )
}

$parts += [ordered]@{
    file = 'roof_accents.json'
    bone = New-Bone -Name 'roof_accents' -Parent 'root' -Pivot @(0, 80, 0) -Cubes @(
        New-Cube -Origin @(-116, 84, -58) -Size @(12, 3, 8) -Uv @(0, 0) -Pivot @(-110, 85.5, -54) -Rotation @(0, 0, 26)
        New-Cube -Origin @(104, 84, -58) -Size @(12, 3, 8) -Uv @(18, 0) -Pivot @(110, 85.5, -54) -Rotation @(0, 0, -26)
        New-Cube -Origin @(-116, 84, -14) -Size @(12, 3, 8) -Uv @(36, 0) -Pivot @(-110, 85.5, -18) -Rotation @(0, 0, -26)
        New-Cube -Origin @(104, 84, -14) -Size @(12, 3, 8) -Uv @(54, 0) -Pivot @(110, 85.5, -18) -Rotation @(0, 0, 26)
        New-Cube -Origin @(-68, 100, -36) -Size @(10, 3, 7) -Uv @(72, 0) -Pivot @(-63, 101.5, -32) -Rotation @(0, 0, 22)
        New-Cube -Origin @(58, 100, -36) -Size @(10, 3, 7) -Uv @(88, 0) -Pivot @(63, 101.5, -32) -Rotation @(0, 0, -22)
        New-Cube -Origin @(-68, 100, 24) -Size @(10, 3, 7) -Uv @(104, 0) -Pivot @(-63, 101.5, 28) -Rotation @(0, 0, -22)
        New-Cube -Origin @(58, 100, 24) -Size @(10, 3, 7) -Uv @(120, 0) -Pivot @(63, 101.5, 28) -Rotation @(0, 0, 22)
        New-Cube -Origin @(-42, 160, -34) -Size @(8, 2, 6) -Uv @(136, 0) -Pivot @(-38, 161, -30) -Rotation @(0, 0, 22)
        New-Cube -Origin @(34, 160, -34) -Size @(8, 2, 6) -Uv @(150, 0) -Pivot @(38, 161, -30) -Rotation @(0, 0, -22)
        New-Cube -Origin @(-42, 160, 28) -Size @(8, 2, 6) -Uv @(164, 0) -Pivot @(-38, 161, 30) -Rotation @(0, 0, -22)
        New-Cube -Origin @(34, 160, 28) -Size @(8, 2, 6) -Uv @(178, 0) -Pivot @(38, 161, 30) -Rotation @(0, 0, 22)
        New-Cube -Origin @(-34, 192, -24) -Size @(8, 2, 6) -Uv @(192, 0) -Pivot @(-30, 193, -20) -Rotation @(0, 0, 20)
        New-Cube -Origin @(26, 192, -24) -Size @(8, 2, 6) -Uv @(206, 0) -Pivot @(30, 193, -20) -Rotation @(0, 0, -20)
        New-Cube -Origin @(-34, 192, 18) -Size @(8, 2, 6) -Uv @(220, 0) -Pivot @(-30, 193, 22) -Rotation @(0, 0, -20)
        New-Cube -Origin @(26, 192, 18) -Size @(8, 2, 6) -Uv @(234, 0) -Pivot @(30, 193, 22) -Rotation @(0, 0, 20)
    )
}

$partDocs = @(
    'This folder contains a separated Blockbench geometry kit for Shengzhu''s palace.'
    ''
    'Files:'
    '- `shengzhu_palace_full.geo.json`: assembled palace reference model.'
    '- other `*.json`: individual editable palace sections.'
    '- `generate_shengzhu_palace.ps1`: source-of-truth generator.'
    ''
    'Design goals:'
    '- based on the supplied concept image, but scaled up into a more imposing post-boss palace.'
    '- split into large architectural chunks so every zone can be adjusted in Blockbench independently.'
    '- keeps the main courtyard and central hall readable for future explorable block construction.'
    ''
    'Parts:'
    '- foundation_base'
    '- courtyard_floor'
    '- outer_ring_walls'
    '- front_gate'
    '- front_stairway'
    '- corner_watchtowers'
    '- left_wing_shell'
    '- right_wing_shell'
    '- side_buttresses'
    '- main_hall_shell'
    '- rear_keep'
    '- grand_roof'
    '- upper_roof'
    '- central_tower'
    '- left_front_pagoda'
    '- right_front_pagoda'
    '- left_mid_pagoda'
    '- right_mid_pagoda'
    '- left_rear_pagoda'
    '- right_rear_pagoda'
    '- roof_accents'
    ''
    'Notes:'
    '- Bedrock / GeckoLib geometry json format, directly openable in Blockbench.'
    "- Texture canvas reserved as ${TextureWidth}x${TextureHeight} for direct palace coloring."
    '- Approximate full footprint is ~17 x 18 blocks with a tall multi-tier silhouette.'
) -join [Environment]::NewLine

[System.IO.File]::WriteAllText((Join-Path $partsDir 'README.md'), $partDocs)

$fullBones = @(
    (New-Bone -Name 'root' -Pivot @(0, 0, 0))
)

for ($i = 0; $i -lt $parts.Count; $i++) {
    $part = $parts[$i]
    $offsetX = ($i % 4) * 512
    $offsetY = [math]::Floor($i / 4) * 512
    Offset-BoneUv -Bone $part.bone -OffsetX $offsetX -OffsetY $offsetY

    $partIdentifier = "geometry.chen_mod.shengzhu_palace.$([System.IO.Path]::GetFileNameWithoutExtension($part.file))"
    $partFile = New-PartFile -Identifier $partIdentifier -Bones @(
        (New-Bone -Name 'root' -Pivot @(0, 0, 0))
        $part.bone
    )
    Write-JsonFile -Path (Join-Path $partsDir $part.file) -Data $partFile
    $fullBones += $part.bone
}

$fullModel = [ordered]@{
    format_version       = '1.16.0'
    'minecraft:geometry' = @(
        [ordered]@{
            description = [ordered]@{
                identifier            = 'geometry.chen_mod.shengzhu_palace'
                texture_width         = $TextureWidth
                texture_height        = $TextureHeight
                visible_bounds_width  = 24
                visible_bounds_height = 20
                visible_bounds_offset = @(0, 8, 0)
            }
            bones = $fullBones
        }
    )
}

Write-JsonFile -Path $fullModelPath -Data $fullModel
Write-JsonFile -Path $assetModelPath -Data $fullModel

Write-Output "Generated $(($parts | Measure-Object).Count) parts"
Write-Output "Full model: $fullModelPath"
Write-Output "Asset model: $assetModelPath"
