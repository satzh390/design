# Floating-Point and Decimal Numbers — Recall Notes

> **Core idea:** Every value is ultimately represented by bits, but a data type defines how those bits are interpreted. A `double` encodes a rounded binary scientific-notation value. Java's `BigDecimal` represents a decimal value using an integer plus a decimal scale.

## 1. Keep these three ideas separate

1. **Machine representation:** bits stored in memory.
2. **Numeric encoding:** the rules that say what those bits mean.
3. **Text representation:** characters such as `"0.1"` in JSON or source code.

The text `0.1` is not itself the bit pattern stored in a `double`. A parser converts the text into a numeric value according to the destination type and conversion rules.

## 2. Binary place values, including fractions

Binary uses powers of two, just as decimal uses powers of ten.

| Position | Decimal value |
|---|---:|
| `2^3` | 8 |
| `2^2` | 4 |
| `2^1` | 2 |
| `2^0` | 1 |
| `2^-1` | 0.5 |
| `2^-2` | 0.25 |
| `2^-3` | 0.125 |
| `2^-4` | 0.0625 |

For example:

`1100.1₂ = 1×8 + 1×4 + 1×0.5 = 12.5₁₀`

**Watch out:** binary `0.1₂` equals decimal `0.5₁₀`. The digits after a radix point depend on the base.

Some decimal fractions terminate in binary; others repeat forever:
- `0.5₁₀ = 0.1₂` exactly.
- `0.125₁₀ = 0.001₂` exactly.
- `0.1₁₀ = 0.0001100110011…₂` repeats forever.

A reduced fraction has a finite binary expansion exactly when its denominator is a power of 2. Since `0.1 = 1/10` and `10 = 2 × 5`, its denominator has a factor of 5, so its binary expansion does not terminate.

## 3. Convert a decimal fraction to binary

A useful hand method for a fraction between 0 and 1 is: repeatedly multiply the remaining fraction by 2. The integer part (0 or 1) is the next binary digit; keep the fractional part for the next step.

For decimal `0.1`:

| Step | Multiply remaining fraction by 2 | Next bit | Remainder |
|---|---:|---:|---:|
| 1 | 0.1 × 2 = 0.2 | 0 | 0.2 |
| 2 | 0.2 × 2 = 0.4 | 0 | 0.4 |
| 3 | 0.4 × 2 = 0.8 | 0 | 0.8 |
| 4 | 0.8 × 2 = 1.6 | 1 | 0.6 |
| 5 | 0.6 × 2 = 1.2 | 1 | 0.2 |
| 6 | 0.2 × 2 = 0.4 | 0 | 0.4 |

Read the next-bit column: `0.00011…₂`. At step 5, remainder `0.2` reappears, so the cycle repeats. The exact expansion is:

`0.1₁₀ = 0.00011001100110011…₂`

This is a way to understand the mathematics, not necessarily the implementation strategy used by every production conversion routine. Generating `n` bits this way takes `n` steps with fixed-size arithmetic. Real parsers can use optimized integer arithmetic to obtain the correctly rounded result without generating an infinite expansion.

## 4. Normalize, like scientific notation

Decimal scientific notation writes `1234 = 1.234 × 10³`. Binary floating point uses the same idea with powers of 2.

`0.0001100110011…₂ = 1.1001100110011…₂ × 2⁻⁴`

This is a normalized binary value:
- The leading significand bit is `1`.
- The exponent is `-4`.
- The bits after the binary point form the fraction/significand tail.

The exponent is not found by searching every possible power one at a time. It can be determined through bit operations and comparisons. Conversion algorithms then determine the significand and the rounding needed for the target format.

## 5. IEEE 754 `double`: the 64-bit layout

Java `double` uses IEEE 754 binary64:

| Field | Width | Purpose |
|---|---:|---|
| Sign | 1 bit | 0 = positive, 1 = negative |
| Biased exponent | 11 bits | Exponent encoded with a bias |
| Fraction field | 52 bits | Significant bits after the implicit leading 1 for normal values |
| **Total** | **64 bits** | 1 + 11 + 52 |

Normal finite values are interpreted as:

`(-1)^sign × (1.fraction) × 2^(storedExponent − 1023)`

The leading `1` is implicit for normal values, so the significand has 53 bits of precision: that leading bit plus the 52 stored fraction bits.

The exponent bias is 1023. For `0.1`, the normalized exponent is `-4`, so:

`stored exponent = -4 + 1023 = 1019`

`1019₁₀ = 01111111011₂`

The sign bit is `0`, because `0.1` is positive.

## 6. The actual stored bits for `double x = 0.1`

The binary expansion of `0.1` repeats forever, but the format has only 52 stored fraction bits. IEEE 754 rounds the value to the nearest representable binary64 value (Java's ordinary floating-point operations use round-to-nearest, ties-to-even).

The resulting 64-bit pattern is:

- Sign: `0`
- Exponent: `01111111011` (decimal 1019)
- Fraction: `1001100110011001100110011001100110011001100110011010` (52 bits)
- Hexadecimal: `0x3FB999999999999A`

Those final bits are the result of rounding. The infinite binary expansion did not suddenly terminate.

The exact real number represented by this stored pattern is:

`0.1000000000000000055511151231257827021181583404541015625`

It is slightly greater than the intended decimal `0.1`.

## 7. How the computer uses the stored value

When a floating-point operation consumes the bits, the hardware follows the format's rules:
1. Read the sign, exponent, and fraction fields.
2. Reconstruct the significand (for a normal value, prepend the implicit `1`).
3. Unbias the exponent.
4. Perform the operation using floating-point arithmetic.
5. Round the result to the target format when required.

The decimal point is not stored as a separate dot. The exponent controls the scale of the binary significand.

This is why the computer can store and calculate with the rounded value efficiently without storing an infinite number of bits.

## 8. Why `0.1 + 0.2` can be surprising

Neither decimal `0.1` nor decimal `0.2` has an exact finite binary64 representation. Each is rounded to a nearby representable value. Adding those stored approximations can produce a result slightly different from the exact decimal `0.3`.

```java
System.out.println(0.1 + 0.2);
// 0.30000000000000004
```

This is not random CPU behavior or a Java bug. It follows from finite binary precision and rounding.

Floating point is excellent for many scientific, graphics, and measurement tasks. It is not automatically the right representation when exact decimal arithmetic is required.

## 9. How Java `BigDecimal` represents decimal values

`BigDecimal` uses a different model. Conceptually it stores:
- an arbitrary-precision unscaled integer;
- a decimal scale.

The value is:

`unscaledValue × 10^(−scale)`

For `123.45`:
- unscaled value = `12345`;
- scale = `2`;
- value = `12345 × 10⁻² = 123.45`.

The unscaled integer `12345` is itself stored using bits (Java's `BigInteger` representation). The scale is also an integer. The power of ten is part of the **interpretation rule**; the computer does not need to store decimal digits as physical decimal hardware.

The same unscaled integer with different scales represents different values:

| Unscaled value | Scale | Decimal value |
|---:|---:|---:|
| 12345 | 0 | 12345 |
| 12345 | 1 | 1234.5 |
| 12345 | 2 | 123.45 |
| 12345 | 3 | 12.345 |

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

`BigDecimal` supports arbitrary precision subject to memory and implementation limits, but division can produce a repeating decimal. Such operations need a rounding mode and/or a scale when an exact finite result is not possible.

## 10. Don't confuse signed integers with floating point

Two's complement is the standard representation used by Java's fixed-width signed integer primitives such as `int` and `long`.

- Positive two's-complement integers use ordinary binary with a leading zero in the signed range.
- Negative integers are encoded using two's complement (invert the bits and add 1, at a fixed width).
- IEEE 754 floating point does **not** encode a negative value by taking the two's complement of the entire floating-point bit pattern. It has a separate sign bit.
- Java `BigInteger` represents arbitrary-precision integer sign and magnitude internally; do not assume it is simply a fixed-width two's-complement bit pattern.

The same bit pattern only has meaning when paired with its encoding rules.

## 11. A 64-bit CPU does not limit every number to 64 bits

Architecture and data type are different concepts.

| Type | Representation | Practical implication |
|---|---|---|
| Java `long` | Fixed 64-bit signed integer | Range is −2⁶³ through 2⁶³−1 |
| Java `double` | Fixed 64-bit binary floating point | About 15–17 significant decimal digits; not all decimal fractions exact |
| Java `BigInteger` | Variable-length integer | Can exceed 64 bits, limited by resources |
| Java `BigDecimal` | Arbitrary-precision unscaled integer + scale | Can represent decimal fractions exactly when constructed appropriately |

A 64-bit CPU can process a number larger than 64 bits using multiple machine-word operations. The CPU's architecture does not force every data type to occupy one 64-bit word.

## 12. JSON: syntax is not a destination type

JSON's number syntax allows values such as `123`, `123.45`, and `1e3`. JSON itself does not require a receiver to map them to Java `double`, `BigDecimal`, or another particular in-memory type.

The application and its JSON parser choose how to deserialize the number. If a Java field is a `double`, the value becomes binary floating point; if it is a `BigDecimal`, the parser can create a decimal representation. Parser configuration can affect this behavior.

For money, choose a decimal representation deliberately and define the rounding policy. Do not assume that a JSON decimal-looking token guarantees exact decimal arithmetic end to end.

## 13. Recall checklist

Try answering these without looking:

1. Why is binary `0.1₂` not decimal `0.1₁₀`?
2. Why does decimal `0.5` terminate in binary but decimal `0.1` repeat?
3. What are the three fields of an IEEE 754 binary64 value?
4. Why is the exponent bias 1023 used, and what stored exponent corresponds to `2⁻⁴`?
5. Why does `double` have 53 bits of significand precision despite having a 52-bit fraction field?
6. What does rounding mean when the exact binary expansion is infinite?
7. How can `BigDecimal` use powers of ten if the machine stores bits?
8. Why does `new BigDecimal("0.1")` differ from `new BigDecimal(0.1)`?
9. Why is two's complement relevant to `long`, but not the encoding scheme for a negative `double`?
10. Why can a 64-bit computer work with a `BigInteger` larger than 64 bits?

### Answers in one line

1. The base changes the meaning of each digit position.
2. A reduced fraction terminates in base 2 only when its denominator is a power of 2.
3. Sign (1), biased exponent (11), fraction (52).
4. Bias = 1023; `−4 + 1023 = 1019`.
5. Normal values have an implicit leading `1` plus 52 stored fraction bits.
6. The infinite value is approximated by the nearest representable value according to the rounding rule.
7. Store an integer and scale; use the scale to interpret the integer as a decimal.
8. The string is exact decimal one-tenth; the double already contains a binary approximation.
9. Floating point has a separate sign bit; two's complement is an integer encoding.
10. The value is spread across multiple machine words and processed by library code.

## Quick memory hook

- **Binary floating point:** sign + exponent + significand; fast and finite precision.
- **BigDecimal:** unscaled integer + base-10 scale; exact decimal representation for finite decimal values.
- **Two's complement:** signed fixed-width integer encoding.
- **64-bit architecture:** CPU design, not a universal 64-bit limit on all values.
