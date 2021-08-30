package com.alcatrazescapee.hexlands.world;

import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

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
