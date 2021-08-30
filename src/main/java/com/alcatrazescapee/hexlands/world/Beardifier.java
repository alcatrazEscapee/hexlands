/*
 * Part of the HexLands mod.
 * Licensed under MIT. See the project LICENSE.txt for details.
 */

package com.alcatrazescapee.hexlands.world;

import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.feature.jigsaw.JigsawJunction;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class Beardifier
{
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], array -> {
        for (int z = 0; z < 24; ++z)
        {
            for (int x = 0; x < 24; ++x)
            {
                for (int y = 0; y < 24; ++y)
                {
                    array[z * 24 * 24 + x * 24 + y] = (float) computeContribution(x - 12, y - 12, z - 12);
                }
            }
        }
    });

    public static void sampleStructureContributions(IChunk chunkIn, StructureManager structureManager, ObjectList<StructurePiece> pieces, ObjectList<JigsawJunction> junctions)
    {
        final ChunkPos chunkPos = chunkIn.getPos();
        final int blockX = chunkPos.getMinBlockX(), blockZ = chunkPos.getMinBlockZ();

        for (Structure<?> structure : Structure.NOISE_AFFECTING_FEATURES)
        {
            structureManager.startsForFeature(SectionPos.of(chunkPos, 0), structure).forEach(start -> {
                for (StructurePiece piece : start.getPieces())
                {
                    if (piece.isCloseToChunk(chunkPos, 12))
                    {
                        if (piece instanceof AbstractVillagePiece)
                        {
                            AbstractVillagePiece villagePiece = (AbstractVillagePiece) piece;
                            JigsawPattern.PlacementBehaviour behavior = villagePiece.getElement().getProjection();
                            if (behavior == JigsawPattern.PlacementBehaviour.RIGID)
                            {
                                pieces.add(villagePiece);
                            }

                            for (JigsawJunction junction : villagePiece.getJunctions())
                            {
                                final int sourceX = junction.getSourceX();
                                final int sourceZ = junction.getSourceZ();
                                if (sourceX > blockX - 12 && sourceZ > blockZ - 12 && sourceX < blockX + 15 + 12 && sourceZ < blockZ + 15 + 12)
                                {
                                    junctions.add(junction);
                                }
                            }
                        }
                        else
                        {
                            pieces.add(piece);
                        }
                    }
                }

            });
        }
    }

    public static double getContribution(int x, int y, int z)
    {
        final int xIndex = x + 12, yIndex = y + 12, zIndex = z + 12;
        if (xIndex >= 0 && xIndex < 24 && yIndex >= 0 && yIndex < 24 && zIndex >= 0 && zIndex < 24)
        {
            return BEARD_KERNEL[zIndex * 24 * 24 + xIndex * 24 + yIndex];
        }
        return 0.0D;
    }

    private static double computeContribution(int x, int y, int z)
    {
        double r = (x * x + z * z);
        double y0 = y + 0.5D;
        double y2 = y0 * y0;
        double f1 = Math.pow(Math.E, -(y2 / 16.0D + r / 16.0D));
        double f2 = -y0 * MathHelper.fastInvSqrt(y2 / 2.0D + r / 2.0D) / 2.0D;
        return f1 * f2;
    }
}
