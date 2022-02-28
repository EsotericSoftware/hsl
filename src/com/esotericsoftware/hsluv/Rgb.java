/* Copyright (c) 2022 Nathan Sweet
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.esotericsoftware.hsluv;

import static com.esotericsoftware.hsluv.Hsl.*;

/** Stores an RGB color. Interpolation is done without losing brightness.
 * @author Nathan Sweet */
public class Rgb {
	public float r, g, b;

	public Rgb () {
	}

	public Rgb (Rgb rgb) {
		set(rgb);
	}

	public Rgb (float r, float g, float b) {
		set(r, g, b);
	}

	public Rgb set (Rgb rgb) {
		this.r = r < 0 ? 0 : (r > 1 ? 1 : r);
		this.g = g < 0 ? 0 : (g > 1 ? 1 : g);
		this.b = b < 0 ? 0 : (b > 1 ? 1 : b);
		return this;
	}

	public Rgb set (float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		return this;
	}

	public Rgb set (int rgb) {
		r = ((rgb & 0xff0000) >>> 16) / 255f;
		g = ((rgb & 0x00ff00) >>> 8) / 255f;
		b = ((rgb & 0x0000ff)) / 255f;
		return this;
	}

	public Rgb lerp (Rgb target, float a) {
		if (a == 0) return this;
		if (a == 1) return set(target);
		float r = toLinear(this.r), g = toLinear(this.g), b = toLinear(this.b);
		float r2 = toLinear(target.r), g2 = toLinear(target.g), b2 = toLinear(target.b);
		float L = rgbToL(r, g, b);
		L += (rgbToL(r2, g2, b2) - L) * a;
		r += (r2 - r) * a;
		g += (g2 - g) * a;
		b += (b2 - b) * a;
		float L2 = rgbToL(r, g, b);
		float scale = L2 < 0.00001f ? 1 : L / L2;
		this.r = fromLinear(r * scale);
		this.g = fromLinear(g * scale);
		this.b = fromLinear(b * scale);
		return this;
	}

	public int toInt () {
		return ((int)(255 * r) << 16) | ((int)(255 * g) << 8) | ((int)(255 * b));
	}

	public boolean equals (Object o) {
		if (o == null) return false;
		Rgb other = (Rgb)o;
		return (int)(255 * r) == (int)(255 * other.r) //
			&& (int)(255 * g) == (int)(255 * other.g) //
			&& (int)(255 * b) == (int)(255 * other.b);
	}

	public int hashCode () {
		int result = (int)(255 * r);
		result = 31 * result + (int)(255 * g);
		return 31 * result + (int)(255 * b);
	}

	public String toString () {
		String value = Integer.toHexString(toInt());
		while (value.length() < 6)
			value = "0" + value;
		return value;
	}

	static private float rgbToL (float r, float g, float b) {
		float Y = minv[1][0] * r + minv[1][1] * g + minv[1][2] * b;
		return Y <= epsilon ? Y * kappa : 1.16f * (float)Math.pow(Y, 1 / 3f) - 0.16f;
	}
}
