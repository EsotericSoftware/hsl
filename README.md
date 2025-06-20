# This project is now obsolete, replaced by [EsotericSoftware/colors](https://github.com/EsotericSoftware/colors).

---

---

---

This Java library provides a class to store a color in the HSLuv color space, interpolate between colors, and to convert between HSLuv and RGB.

This library is based on the work by Alexei Boronine at [hsluv.org](https://www.hsluv.org/) and [hsluv-java](https://github.com/hsluv/hsluv-java). This library differs in that it is slightly more efficient, conversion to and from RGB do not allocate, and a novel method of interpolation has been added. Interpolation is done without introducing new intermediary hues and without losing lightness.

## Interpolation

Simple interpolation of HSL mixes around the hue circle clockwise or counterclockwise, causing other hues to appear during the mix. Also, HSL can describe colors with components that can't be seen but show up during interpolation. For example, 0 hue (red), 100% saturation, and 100% lightness gives white but simple interpolation from this color can cause the red to show as the lightness is reduced.

Simple interpolation of RGB doesn't have those problems but results in a darker color in the middle of the mix. This can be improved by interpolating on linear rather than gamma corrected values but that worsens some mixes, such black to white. Mark Ransom provides a [potential solution](https://stackoverflow.com/a/49321304/187883), but it crushes the start and end of the black to white gradient.

This library interpolates between two HSLuv colors by converting to linear RGB, interpolating, then converting back to HSLuv. The lightness of the start and end HSLuv colors are  interpolated and used as the lightness of the interpolated color.

This library interpolates between two RGB colors by interpolating linear RGB. Additionally, the lightness of the start and end RGB colors are computed, interpolated, and used to correct the lightness of the interpolated color. The gives good RGB interpolation without requiring the full conversion to and from HSLuv.

RGB:

<a href="https://raw.githubusercontent.com/EsotericSoftware/hsl/refs/heads/main/images/RGB.png"><img src="https://raw.githubusercontent.com/EsotericSoftware/hsl/refs/heads/main/images/RGB.png" width="320"></a>

RGB with L fix:

<a href="https://raw.githubusercontent.com/EsotericSoftware/hsl/refs/heads/main/images/RGB-with-L-fix.png"><img src="https://raw.githubusercontent.com/EsotericSoftware/hsl/refs/heads/main/images/RGB-with-L-fix.png" width="320"></a>
