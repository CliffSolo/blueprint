
# How a 4-Stroke Engine Works

A [Blueprint](../../README.md) course: **suck, squeeze, bang, blow.**
Watch a car engine turn fuel into motion, one stroke at a time. ~25 minutes, 5 lessons, 4 animated labs.

<!-- Add docs/engine.gif (a short screen recording of labs/engine.html) for the hero image. -->

## Lessons

| # | Lesson | Lab |
|---|---|---|
| 1 | The big idea: parts of an engine | `engine.html`, a labelled cutaway that runs continuously |
| 2 | Four strokes, one at a time | `engine.html?mode=step`, one stroke per tap |
| 3 | Valves and timing | `timing.html`, crank and cam gears (2:1), timing wheel, valve overlap, spark advance vs RPM |
| 4 | The squeeze: size and compression | `compression.html`, compression-ratio slider with gas particles, pressure and temperature |
| 5 | More cylinders, smoother power | `cylinders.html`, 1/2/4 cylinders, firing order 1-3-4-2, push-on-crank graph |

Every lesson ends with quick-check questions.

## Try the labs in a browser

The labs are plain HTML with no dependencies. Open any file in `labs/` directly, or add `?theme=dark`.

## Format

`course.json` lists the lessons; each `lessons/*.json` is a list of blocks (list, text, callout, steps, math, lab, quiz). This folder lives in the Blueprint repo's `courses/`, so the app downloads it automatically.

## The physics, simplified

- Piston position uses the real slider-crank formula: `y = r·cos θ + √(L² − r²·sin² θ)`.
- Compression pressure and temperature use the squeeze-without-burning rule: `P = CR^1.3` bar, `T = 300 K × CR^0.3`.
- The push-on-crank graph is a simplified torque curve, accurate in shape (power stroke positive, others dragging), not in exact numbers.

## License

[MIT](LICENSE)
