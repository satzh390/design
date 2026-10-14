# Floating-Point and Decimal Numbers

This document explains how computers represent fractional numbers, why some decimal fractions cannot be represented exactly in binary floating point, how IEEE 754 `double` stores a value, and how Java's `BigDecimal` represents decimal values.

## 1. Bits are physical storage; types define interpretation

Everything in memory is stored as bits. The same bits can mean different things depending on the type and encoding rules.

Keep three things separate:

1. **Text representation:** characters such as `"0.1"` in source code or JSON.
2. **Numeric encoding:** the rules used to turn the text into a value of a particular type.
3. **Machine representation:** the bits stored in memory.

A Java `double` does not store the characters `0.1`. It stores a binary floating-point approximation of that value. A Java `BigDecimal` uses a different representation: an integer and a decimal scale.

## 2. Binary place values include fractions

Binary uses powers of two, just as decimal uses powers of ten.

| Position | Value |
|---|---:|
| `2^3` | 8 |
| `2^2` | 4 |
| `2^1` | 2 |
| `2^0` | 1 |
| `2^-1` | 0.5 |
| `2^-2` | 0.25 |
| `2^-3` | 0.125 |
| `2^-4` | 0.0625 |

For example, `1100.1₂` means:

`1×8 + 1×4 + 1×0.5 = 12.5₁₀`

Be careful: binary `0.1₂` equals decimal `0.5₁₀`. A digit's value depends on the number system.

Some decimal fractions terminate in binary:
- `0.5₁₀ = 0.1₂`
- `0.125₁₀ = 0.001₂`

Others repeat forever:
- `0.1₁₀ = 0.00011001100110011…₂`

A reduced rational fraction has a finite binary expansion only if its denominator is a power of 2. Since `0.1 = 1/10`, and `10 = 2 × 5`, its denominator contains a factor other than 2. Its binary expansion therefore does not terminate.

## 3. Convert a fractional number to binary

A useful way to understand the conversion of a fraction between 0 and 1 is to repeatedly multiply the remaining fraction by 2. The integer part (0 or 1) is the next binary digit; keep the fractional part for the next step.

For decimal `0.1`:

| Step | Multiply remaining fraction by 2 | Next bit | Remainder |
|---|---:|---:|---:|
| 1 | 0.1 × 2 = 0.2 | 0 | 0.2 |
| 2 | 0.2 × 2 = 0.4 | 0 | 0.4 |
| 3 | 0.4 × 2 = 0.8 | 0 | 0.8 |
| 4 | 0.8 × 2 = 1.6 | 1 | 0.6 |
| 5 | 0.6 × 2 = 1.2 | 1 | 0.2 |
| 6 | 0.2 × 2 = 0.4 | 0 | 0.4 |

Read the next-bit column: `0.00011…₂`. At step 5, the remainder `0.2` appears again, so the sequence repeats:

`0.1₁₀ = 0.00011001100110011…₂`

This method explains the mathematics. It is not necessarily the exact algorithm used by every production parser. Generating `n` bits this way takes `n` steps with fixed-size arithmetic. Real conversion algorithms can use optimized integer arithmetic to compute the correctly rounded result without generating an infinite expansion.

## 4. Normalize the binary number

Floating point uses an idea similar to scientific notation. In decimal:

`1234 = 1.234 × 10^3`

In binary:

`0.0001100110011…₂ = 1.1001100110011…₂ × 2^-4`

This is a normalized binary value:
- The leading significand bit is `1`.
- The exponent is `-4`.
- The bits after the binary point are the remaining significand digits.

The computer does not need to test every exponent one by one. It can determine the exponent through bit operations and comparisons, then determine the significand and rounding needed for the target format.

## 5. IEEE 754 `double`: the 64-bit layout

Java's `double` uses IEEE 754 binary64.

| Field | Width | Purpose |
|---|---:|---|
| Sign | 1 bit | 0 = positive; 1 = negative |
| Biased exponent | 11 bits | Encodes the exponent using a bias |
| Fraction field | 52 bits | Stores the significand digits after the implicit leading 1 for normal values |
| **Total** | **64 bits** | **1 + 11 + 52** |

A normal finite value is interpreted as:

`(-1)^sign × (1.fraction) × 2^(storedExponent − 1023)`

For normal values, the leading `1` is implicit, so the significand has 53 bits of precision: one implicit bit plus 52 stored fraction bits.

The exponent bias is 1023. For decimal `0.1`, the normalized binary exponent is `-4`, so:

`stored exponent = -4 + 1023 = 1019`

`1019₁₀ = 01111111011₂`

The sign bit is `0`, because the number is positive.

## 6. The actual stored bits for `double x = 0.1`

The binary expansion of decimal `0.1` repeats forever, but a `double` has only 52 stored fraction bits. IEEE 754 rounds the value to the nearest representable binary64 value; the default rounding mode for ordinary Java floating-point operations is round-to-nearest, ties-to-even.

For `double x = 0.1`, the fields are:

- Sign: `0`
- Exponent: `01111111011` (decimal 1019)
- Fraction: `1001100110011001100110011001100110011001100110011010` (52 bits)
- Hexadecimal bit pattern: `0x3FB999999999999A`

The final bits are the result of rounding. The infinite binary expansion did not terminate.

The exact value represented by those bits is:

`0.1000000000000000055511151231257827021181583404541015625`

This is slightly greater than the intended decimal value `0.1`.

## 7. How the computer uses the stored value

When a floating-point operation consumes the bits, the hardware follows the format's rules:

1. Read the sign, exponent, and fraction fields.
2. Reconstruct the significand (for a normal value, prepend the implicit `1`).
3. Unbias the exponent.
4. Perform the operation using floating-point arithmetic.
5. Round the result to the target format when required.

The binary point is not stored as a separate character. The exponent controls the scale of the binary significand.

This lets the computer store and calculate with the rounded value efficiently, without storing an infinite number of bits.

## 8. Why `0.1 + 0.2` can be surprising

Neither decimal `0.1` nor decimal `0.2` has an exact finite binary64 representation. Each is rounded to a nearby representable value. Adding those approximations can produce a result slightly different from exact decimal `0.3`.

```java
System.out.println(0.1 + 0.2);
// 0.30000000000000004
```

This is not random behavior or a Java bug. It follows from finite binary precision and rounding.

Floating point is useful for scientific computing, graphics, and measurements. It is not automatically the right representation when exact decimal arithmetic is required.

## 9. How Java `BigDecimal` represents decimal values

`BigDecimal` uses a different model. Conceptually, it stores:
- an arbitrary-precision unscaled integer;
- a decimal scale.

Its value is:

`unscaledValue × 10^(-scale)`

For `123.45`:
- Unscaled value = `12345`
- Scale = `2`
- Value = `12345 × 10^-2 = 123.45`

The unscaled integer `12345` is itself stored using bits (Java's `BigInteger` representation). The scale is also an integer. The power of ten is part of the **interpretation rule**; the computer does not need to store decimal digits as physical decimal hardware.

The same unscaled integer with different scales represents different values:

| Unscaled value | Scale | Decimal value |
|---:|---:|---:|
| 12345 | 0 | 12345 |
| 12345 | 1 | 1234.5 |
| 12345 | 2 | 123.45 |
| 12345 | 3 | 12.345 |
| 12345 | 4 | 1.2345 |

`BigDecimal` can preserve trailing-zero scale: `new BigDecimal("123.4500")` has scale 4, while `new BigDecimal("123.45")` has scale 2. They compare numerically equal using `compareTo`, though `equals` also considers scale.

### Construct decimal values safely

```java
BigDecimal exact = new BigDecimal("0.1")
        .add(new BigDecimal("0.2"));

System.out.println(exact); // 0.3

BigDecimal fromDouble = new BigDecimal(0.1);
System.out.println(fromDouble);
// 0.1000000000000000055511151231257827021181583404541015625
```

Prefer a decimal string or `BigDecimal.valueOf(double)` when you want the canonical decimal representation of a `double`. The constructor `new BigDecimal(double)` captures the exact decimal value of the binary floating-point number.

`BigDecimal` supports arbitrary precision subject to memory and implementation limits, but division can produce a repeating decimal. Such operations need a rounding mode and/or scale when an exact finite result is not possible.

## 10. Signed integers and floating point use different encodings

Two's complement is the standard representation used by Java's fixed-width signed integer primitives such as `int` and `long`.

- Positive two's-complement integers use ordinary binary with a leading zero in the signed range.
- Negative integers are encoded using two's complement at a fixed width: invert the bits and add 1.
- IEEE 754 floating point does **not** encode a negative value by taking the two's complement of the entire floating-point bit pattern. It has a separate sign bit.
- Java `BigInteger` represents arbitrary-precision integer sign and magnitude internally; do not assume it is simply a fixed-width two's-complement bit pattern.

A bit pattern only has meaning when paired with its encoding rules.

## 11. A 64-bit CPU does not limit every number to 64 bits

Architecture and data type are different concepts.

| Type | Representation | Practical implication |
|---|---|---|
| Java `long` | Fixed 64-bit signed integer | Range: −2⁶³ through 2⁶³−1 |
| Java `double` | Fixed 64-bit binary floating point | About 15–17 significant decimal digits; not all decimal fractions are exact |
| Java `BigInteger` | Variable-length integer | Can exceed 64 bits, limited by resources |
| Java `BigDecimal` | Arbitrary-precision unscaled integer + scale | Can represent finite decimal fractions exactly when constructed appropriately |

A 64-bit CPU can process numbers larger than 64 bits using multiple machine-word operations. The architecture does not force every data type to occupy one 64-bit word.

## 12. JSON number syntax does not specify a destination type

JSON permits numeric tokens such as `123`, `123.45`, and `1e3`. JSON itself does not require a receiver to map them to Java `double`, `BigDecimal`, or another particular in-memory type.

The application and its JSON parser choose how to deserialize the number. If a Java field is a `double`, it becomes binary floating point; if it is a `BigDecimal`, the parser can create a decimal representation. Parser configuration can affect this behavior.

For money, choose a decimal representation deliberately and define the rounding policy. A JSON token with decimal digits does not guarantee exact decimal arithmetic throughout the whole application.

## Summary

- **Binary floating point:** sign + exponent + significand; efficient, finite precision.
- **`BigDecimal`:** unscaled integer + base-10 scale; supports exact representation of finite decimal values.
- **Two's complement:** encoding for fixed-width signed integers, not the encoding for negative `double` values.
- **64-bit architecture:** describes CPU capabilities; it does not limit all numbers to 64 bits.
