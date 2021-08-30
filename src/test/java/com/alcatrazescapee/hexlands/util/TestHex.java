package com.alcatrazescapee.hexlands.util;

import org.junit.jupiter.api.Test;
import org.quicktheories.WithQuickTheories;
import org.quicktheories.dsl.TheoryBuilder3;

import static org.junit.jupiter.api.Assertions.*;

public class TestHex implements WithQuickTheories
{
    private static final double epsilon = 0.000000001;

    @Test
    public void testOriginCoords()
    {
        qt()
            .forAll(doubles().between(1, 100))
            .asWithPrecursor(size -> new Hex(0, 0, size))
            .checkAssert((size, hex) -> {
                assertEquals(0, hex.q());
                assertEquals(0, hex.r());
                assertEquals(0, hex.x());
                assertEquals(0, hex.z());
            });
    }

    @Test
    public void testOriginRadius()
    {
        qt()
            .forAll(doubles().between(1, 100))
            .asWithPrecursor(size -> new Hex(0, 0, size))
            .checkAssert((size, hex) -> {
                assertEquals(0, hex.radius(0, 0), epsilon);
                assertEquals(1, hex.radius(size, 0), epsilon); // corners along the q axis
                assertEquals(1, hex.radius(-size, 0), epsilon);
            });
    }

    @Test
    public void testRadiusAtCenterIsZero()
    {
        points()
            .as(Hex::new)
            .check(hex -> hex.radius(hex.x(), hex.z()) < epsilon);
    }

    @Test
    public void testRadiusLessThanOne()
    {
        points()
            .asWithPrecursor(Hex::blockToHex)
            .check((x, z, size, hex) -> hex.radius(x, z) <= 1);
    }

    @Test
    public void testRadiusPositive()
    {
        points()
            .asWithPrecursor(Hex::blockToHex)
            .check((x, z, size, hex) -> hex.radius(x, z) >= 0);
    }

    private TheoryBuilder3<Integer, Integer, Double> points()
    {
        return qt().forAll(
            integers().between(-100000, 100000),
            integers().between(-100000, 100000),
            doubles().between(10, 100));
    }
}
