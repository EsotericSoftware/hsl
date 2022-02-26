/* Copyright (c) 2016 Alexei Boronine
 * Copyright (c) 2022 Nathan Sweet
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

/** Stores a color in the HSLuv color space. Provides conversion to and from RGB. Interpolation is done without introducing new
 * intermediary hues and without losing brightness. Conversion and interpolation do not allocate.
 * <p>
 * Hue is in degrees, 0-360. Saturation and lightness are percentages, 0-1.
 * <p>
 * Based on: https://github.com/hsluv/hsluv-java/ and https://www.hsluv.org/
 * @author Nathan Sweet */
public class Hsl {
	static private final float[][] m = new float[][] { //
		new float[] {3.240969941904521f, -1.537383177570093f, -0.498610760293f},
		new float[] {-0.96924363628087f, 1.87596750150772f, 0.041555057407175f},
		new float[] {0.055630079696993f, -0.20397695888897f, 1.056971514242878f}};
	static private final float[][] minv = new float[][] { //
		new float[] {0.41239079926595f, 0.35758433938387f, 0.18048078840183f},
		new float[] {0.21263900587151f, 0.71516867876775f, 0.072192315360733f},
		new float[] {0.019330818715591f, 0.11919477979462f, 0.95053215224966f}};
	static private final float refU = 0.19783000664283f, refV = 0.46831999493879f;
	static private final float kappa = 9.032962962f, epsilon = 0.0088564516f;
	static private final float radDeg = (float)(180f / Math.PI);
	static private final float degRad = (float)(Math.PI / 180);

	public float h, s, l;
	private final Rgb rgb = new Rgb();

	public Hsl () {
	}

	public Hsl (Hsl hsl) {
		setHsl(hsl);
	}

	public Hsl (float h, float s, float l) {
		setHsl(h, s, l);
	}

	public Hsl setHsl (Hsl hsl) {
		h = hsl.h;
		s = hsl.s;
		l = hsl.l;
		return this;
	}

	public Hsl setHsl (float h, float s, float l) {
		this.h = h < 0 ? 0 : (h > 360 ? 360 : h);
		this.s = s < 0 ? 0 : (s > 1 ? 1 : s);
		this.l = l < 0 ? 0 : (l > 1 ? 1 : l);
		return this;
	}

	public Hsl setRgb (Rgb rgb) {
		setRgb(rgb.r, rgb.g, rgb.b);
		return this;
	}

	public Hsl setRgb (float r, float g, float b) {
		setRgb(r, g, b, false);
		return this;
	}

	public Hsl setRgb (int rgb) {
		float r = ((rgb & 0xff0000) >>> 16) / 255f;
		float g = ((rgb & 0x00ff00) >>> 8) / 255f;
		float b = ((rgb & 0x0000ff)) / 255f;
		return setRgb(r, g, b);
	}

	private Hsl setRgb (float r, float g, float b, boolean keepL) {
		// RGB to XYZ
		r = toLinear(r);
		g = toLinear(g);
		b = toLinear(b);
		float X = dot(minv[0], r, g, b), Y = dot(minv[1], r, g, b), Z = dot(minv[2], r, g, b);

		// XYZ to Luv
		float L = keepL ? l : (Y <= epsilon ? Y * kappa : 1.16f * (float)Math.pow(Y, 1 / 3f) - 0.16f), U, V;
		if (L < 0.00001f) {
			L = 0;
			U = 0;
			V = 0;
		} else {
			U = 13 * L * (4 * X / (X + 15 * Y + 3 * Z) - refU);
			V = 13 * L * (9 * Y / (X + 15 * Y + 3 * Z) - refV);
		}

		// Luv to Lch
		float C = (float)Math.sqrt(U * U + V * V);
		if (C < 0.00001f)
			h = 0;
		else {
			h = (float)Math.atan2(V, U) * radDeg;
			if (h < 0) h += 360;
		}

		// Lch to HSLuv
		if (L > 0.99999f) {
			s = 0;
			l = 1;
		} else if (L < 0.00001f) {
			s = 0;
			l = 0;
		} else {
			s = Math.min(C / maxChromaForLH(L, h * degRad), 1);
			l = L;
		}
		return this;
	}

	private Rgb getRgbLinear (Rgb rgb) {
		float Hrad = h * degRad, L = l;

		// HSLuv to Lch
		float C;
		if (L > 0.99999f) {
			L = 1;
			C = 0;
		} else if (L < 0.00001f) {
			L = 0;
			C = 0;
		} else
			C = maxChromaForLH(L, Hrad) * s;

		// Lch to Luv
		float U = (float)Math.cos(Hrad) * C;
		float V = (float)Math.sin(Hrad) * C;

		// Luv to XYZ
		float X, Y, Z;
		if (L < 0.00001f) {
			X = 0;
			Y = 0;
			Z = 0;
		} else {
			if (L <= 0.08f)
				Y = L / kappa;
			else {
				Y = (L + 0.16f) / 1.16f;
				Y *= Y * Y;
			}
			float varU = U / (13 * L) + refU;
			float varV = V / (13 * L) + refV;
			X = 9 * varU * Y / (4 * varV);
			Z = (3 * Y / varV) - X / 3 - 5 * Y;
		}

		// XYZ to RGB
		rgb.r = dot(m[0], X, Y, Z);
		rgb.g = dot(m[1], X, Y, Z);
		rgb.b = dot(m[2], X, Y, Z);
		return rgb;
	}

	/** Always returns the same Rgb instance. */
	public Rgb getRgb () {
		return getRgb(rgb);
	}

	public Rgb getRgb (Rgb rgb) {
		getRgbLinear(rgb);
		rgb.r = fromLinear(rgb.r);
		rgb.g = fromLinear(rgb.g);
		rgb.b = fromLinear(rgb.b);
		return rgb;
	}

	public Hsl lerp (Hsl target, float a) {
		l += (target.l - l) * a;
		Rgb linear = getRgbLinear(rgb).lerp(target.getRgbLinear(target.rgb), a);
		setRgb(fromLinear(linear.r), fromLinear(linear.g), fromLinear(linear.b), true);
		return this;
	}

	public boolean equals (Object o) {
		if (o == null) return false;
		Hsl other = (Hsl)o;
		return Float.floatToIntBits(h) == Float.floatToIntBits(other.h) //
			&& Float.floatToIntBits(s) == Float.floatToIntBits(other.s) //
			&& Float.floatToIntBits(l) == Float.floatToIntBits(other.l);
	}

	public int hashCode () {
		int result = Float.floatToRawIntBits(h);
		result = 31 * result + Float.floatToRawIntBits(s);
		return 31 * result + Float.floatToRawIntBits(l);
	}

	public String toString () {
		return h + "," + s + "," + l;
	}

	static private float maxChromaForLH (float L, float Hrad) {
		float sin = (float)Math.sin(Hrad);
		float cos = (float)Math.cos(Hrad);
		float sub1 = (L + 0.16f) / 1.16f;
		sub1 *= sub1 * sub1;
		float sub2 = sub1 > epsilon ? sub1 : L / kappa;
		float min = Float.MAX_VALUE;
		for (int i = 0; i < 3; i++) {
			float m1 = m[i][0] * sub2, m2 = m[i][1] * sub2, m3 = m[i][2] * sub2;
			for (int t = 0; t < 2; t++) {
				float top1 = 2845.17f * m1 - 948.39f * m3;
				float top2 = (8384.22f * m3 + 7698.60f * m2 + 7317.18f * m1 - 7698.60f * t) * L;
				float bottom = (6322.60f * m3 - 1264.52f * m2) + 1264.52f * t;
				float length = intersectLength(sin, cos, top1 / bottom, top2 / bottom);
				if (length >= 0) min = Math.min(min, length);
			}
		}
		return min;
	}

	static private float intersectLength (float sin, float cos, float line1, float line2) {
		return line2 / (sin - line1 * cos);
	}

	static private float fromLinear (float value) {
		return value <= 0.0031308f ? value * 12.92f : (float)(Math.pow(value, 1 / 2.4f) * 1.055f - 0.055f);
	}

	static private float toLinear (float value) {
		return value <= 0.04045f ? value / 12.92f : (float)Math.pow((value + 0.055f) / 1.055f, 2.4f);
	}

	static private float dot (float[] a, float b0, float b1, float b2) {
		return a[0] * b0 + a[1] * b1 + a[2] * b2;
	}

	static public class Rgb {
		public float r, g, b;

		public Rgb lerp (Rgb target, float a) {
			r += (target.r - r) * a;
			g += (target.g - g) * a;
			b += (target.b - b) * a;
			return this;
		}
	}
}
