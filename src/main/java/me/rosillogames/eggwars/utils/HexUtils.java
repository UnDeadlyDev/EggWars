package me.rosillogames.eggwars.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

public final class HexUtils {

    private static final int CHARS_UNTIL_LOOP = 30;
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<(?<type>rainbow|r)(#(?<speed>\\d+))?(:(?<saturation>\\d*\\.?\\d+))?(:(?<brightness>\\d*\\.?\\d+))?(:(?<loop>l|L|loop))?>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<(?<type>gradient|g)(#(?<speed>\\d+))?(?<hex>(:#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})){2,})(:(?<loop>l|L|loop))?>");
    private static final List<Pattern> HEX_PATTERNS = Arrays.asList(
            Pattern.compile("<#([A-Fa-f0-9]){6}>"),   // <#FFFFFF>
            Pattern.compile("\\{#([A-Fa-f0-9]){6}}"), // {#FFFFFF}
            Pattern.compile("&#([A-Fa-f0-9]){6}"),    // &#FFFFFF
            Pattern.compile("#([A-Fa-f0-9]){6}")      // #FFFFFF
    );

    private static final Pattern STOP = Pattern.compile(
            "<(rainbow|r)(#(\\d+))?(:(\\d*\\.?\\d+))?(:(\\d*\\.?\\d+))?(:(l|L|loop))?>|" +
                    "<(gradient|g)(#(\\d+))?((:#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})){2,})(:(l|L|loop))?>|" +
                    "(&[a-f0-9r])|" +
                    "<#([A-Fa-f0-9]){6}>|" +
                    "\\{#([A-Fa-f0-9]){6}}|" +
                    "&#([A-Fa-f0-9]){6}|" +
                    "#([A-Fa-f0-9]){6}|" +
                    ChatColor.COLOR_CHAR
    );

    private HexUtils() {

    }

    private static String getCaptureGroup(Matcher matcher, String group) {
        try {
            return matcher.group(group);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return null;
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorify(message));
    }

    private static boolean isFormat(ChatColor color) {
        return color == ChatColor.MAGIC
                || color == ChatColor.BOLD
                || color == ChatColor.ITALIC
                || color == ChatColor.STRIKETHROUGH
                || color == ChatColor.UNDERLINE;
    }

    public static String colorify(String message) {
        String parsed = message;
        parsed = parseRainbow(parsed);
        parsed = parseGradients(parsed);
        parsed = parseHex(parsed);
        parsed = parseLegacy(parsed);
        return parsed;
    }

    private static String parseRainbow(String message) {
        String parsed = message;

        Matcher matcher = RAINBOW_PATTERN.matcher(parsed);
        while (matcher.find()) {
            StringBuilder parsedRainbow = new StringBuilder();

            // Possible parameters and their defaults
            int speed = -1;
            float saturation = 1.0F;
            float brightness = 1.0F;
            boolean looping = getCaptureGroup(matcher, "looping") != null;

            String speedGroup = getCaptureGroup(matcher, "speed");
            if (speedGroup != null) {
                try {
                    speed = Integer.parseInt(speedGroup);
                } catch (NumberFormatException ignored) { }
            }

            String saturationGroup = getCaptureGroup(matcher, "saturation");
            if (saturationGroup != null) {
                try {
                    saturation = Float.parseFloat(saturationGroup);
                } catch (NumberFormatException ignored) { }
            }

            String brightnessGroup = getCaptureGroup(matcher, "brightness");
            if (brightnessGroup != null) {
                try {
                    brightness = Float.parseFloat(brightnessGroup);
                } catch (NumberFormatException ignored) { }
            }

            int stop = findStop(parsed, matcher.end());
            String content = parsed.substring(matcher.end(), stop);
            int contentLength = content.length();
            char[] chars = content.toCharArray();
            for (int i = 0; i < chars.length - 1; i++)
                if (chars[i] == '&' && "KkLlMmNnOoRr".indexOf(chars[i + 1]) > -1)
                    contentLength -= 2;

            int length = looping ? Math.min(contentLength, CHARS_UNTIL_LOOP) : contentLength;

            ColorGenerator rainbow;
            if (speed == -1) {
                rainbow = new Rainbow(length, saturation, brightness);
            } else {
                rainbow = new AnimatedRainbow(length, saturation, brightness, speed);
            }

            String compoundedFormat = ""; // Carry the format codes through the rainbow gradient
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '&' && i + 1 < chars.length) {
                    char next = chars[i + 1];
                    ChatColor color = ChatColor.getByChar(next);
                    if (color != null && isFormat(color)) {
                        compoundedFormat += String.valueOf(ChatColor.COLOR_CHAR) + next;
                        i++; // Skip next character
                        continue;
                    }
                }
                parsedRainbow.append(translateHex(rainbow.next())).append(compoundedFormat).append(c);
            }

            String before = parsed.substring(0, matcher.start());
            String after = parsed.substring(stop);
            parsed = before + parsedRainbow + after;
            matcher = RAINBOW_PATTERN.matcher(parsed);
        }

        return parsed;
    }

    private static String parseGradients(String message) {
        String parsed = message;

        Matcher matcher = GRADIENT_PATTERN.matcher(parsed);
        while (matcher.find()) {
            StringBuilder parsedGradient = new StringBuilder();

            int speed = -1;
            boolean looping = getCaptureGroup(matcher, "loop") != null;

            List<Color> hexSteps = Arrays.stream(getCaptureGroup(matcher, "hex").substring(1).split(":"))
                    .map(x -> x.length() != 4 ? x : String.format("#%s%s%s%s%s%s", x.charAt(1), x.charAt(1), x.charAt(2), x.charAt(2), x.charAt(3), x.charAt(3)))
                    .map(Color::decode)
                    .collect(Collectors.toList());

            String speedGroup = getCaptureGroup(matcher, "speed");
            if (speedGroup != null) {
                try {
                    speed = Integer.parseInt(speedGroup);
                } catch (NumberFormatException ignored) { }
            }

            int stop = findStop(parsed, matcher.end());
            String content = parsed.substring(matcher.end(), stop);
            int contentLength = content.length();
            char[] chars = content.toCharArray();
            for (int i = 0; i < chars.length - 1; i++)
                if (chars[i] == '&' && "KkLlMmNnOoRr".indexOf(chars[i + 1]) > -1)
                    contentLength -= 2;

            int length = looping ? Math.min(contentLength, CHARS_UNTIL_LOOP) : contentLength;
            ColorGenerator gradient;
            if (speed == -1) {
                gradient = new Gradient(hexSteps, length);
            } else {
                gradient = new AnimatedGradient(hexSteps, length, speed);
            }

            String compoundedFormat = "";
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '&' && i + 1 < chars.length) {
                    char next = chars[i + 1];
                    ChatColor color = ChatColor.getByChar(next);
                    if (color != null && isFormat(color)) {
                        compoundedFormat += String.valueOf(ChatColor.COLOR_CHAR) + next;
                        i++; // Skip next character
                        continue;
                    }
                }
                parsedGradient.append(translateHex(gradient.next())).append(compoundedFormat).append(c);
            }

            String before = parsed.substring(0, matcher.start());
            String after = parsed.substring(stop);
            parsed = before + parsedGradient + after;
            matcher = GRADIENT_PATTERN.matcher(parsed);
        }

        return parsed;
    }

    private static String parseHex(String message) {
        String parsed = message;

        for (Pattern pattern : HEX_PATTERNS) {
            Matcher matcher = pattern.matcher(parsed);
            while (matcher.find()) {
                String color = translateHex(cleanHex(matcher.group()));
                String before = parsed.substring(0, matcher.start());
                String after = parsed.substring(matcher.end());
                parsed = before + color + after;
                matcher = pattern.matcher(parsed);
            }
        }

        return parsed;
    }

    private static String parseLegacy(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static int findStop(String content, int searchAfter) {
        Matcher matcher = STOP.matcher(content);
        while (matcher.find()) {
            if (matcher.start() > searchAfter)
                return matcher.start();
        }
        return content.length();
    }

    private static String cleanHex(String hex) {
        if (hex.startsWith("<") || hex.startsWith("{")) {
            return hex.substring(1, hex.length() - 1);
        } else if (hex.startsWith("&")) {
            return hex.substring(1);
        } else {
            return hex;
        }
    }

    private static String translateHex(String hex) {
        return ChatColor.of(hex).toString();
    }

    private static String translateHex(Color color) {
        return ChatColor.of(color).toString();
    }

    private interface ColorGenerator {
        Color next();
    }

    public static class Gradient implements ColorGenerator {

        private final List<TwoStopGradient> gradients;
        private final int steps;
        protected long step;

        public Gradient(List<Color> colors, int steps) {
            if (colors.size() < 2)
                throw new IllegalArgumentException("Must provide at least 2 colors");

            this.gradients = new ArrayList<>();
            this.steps = steps - 1;
            this.step = 0;

            float increment = (float) this.steps / (colors.size() - 1);
            for (int i = 0; i < colors.size() - 1; i++)
                this.gradients.add(new TwoStopGradient(colors.get(i), colors.get(i + 1), increment * i, increment * (i + 1)));
        }

        @Override
        public Color next() {
            int adjustedStep = (int) Math.round(Math.abs(((2 * Math.asin(Math.sin(this.step * (Math.PI / (2 * this.steps))))) / Math.PI) * this.steps));

            Color color;
            if (this.gradients.size() < 2) {
                color = this.gradients.get(0).colorAt(adjustedStep);
            } else {
                float segment = (float) this.steps / this.gradients.size();
                int index = (int) Math.min(Math.floor(adjustedStep / segment), this.gradients.size() - 1);
                color = this.gradients.get(index).colorAt(adjustedStep);
            }

            this.step++;
            return color;
        }

        private static class TwoStopGradient {

            private final Color startColor;
            private final Color endColor;
            private final float lowerRange;
            private final float upperRange;

            private TwoStopGradient(Color startColor, Color endColor, float lowerRange, float upperRange) {
                this.startColor = startColor;
                this.endColor = endColor;
                this.lowerRange = lowerRange;
                this.upperRange = upperRange;
            }

            public Color colorAt(int step) {
                return new Color(
                        this.calculateHexPiece(step, this.startColor.getRed(), this.endColor.getRed()),
                        this.calculateHexPiece(step, this.startColor.getGreen(), this.endColor.getGreen()),
                        this.calculateHexPiece(step, this.startColor.getBlue(), this.endColor.getBlue())
                );
            }

            private int calculateHexPiece(int step, int channelStart, int channelEnd) {
                float range = this.upperRange - this.lowerRange;
                float interval = (channelEnd - channelStart) / range;
                return Math.round(interval * (step - this.lowerRange) + channelStart);
            }

        }

    }

    public static class AnimatedGradient extends Gradient {

        public AnimatedGradient(List<Color> colors, int steps, int speed) {
            super(colors, steps);

            this.step = System.currentTimeMillis() / speed;
        }

    }

    public static class Rainbow implements ColorGenerator {

        protected final float hueStep, saturation, brightness;
        protected float hue;

        public Rainbow(int totalColors, float saturation, float brightness) {
            if (totalColors < 1)
                totalColors = 1;

            this.hueStep = 1.0F / totalColors;
            this.saturation = Math.max(0, Math.min(1, saturation));
            this.brightness = Math.max(0, Math.min(1, brightness));
            this.hue = 0;
        }

        @Override
        public Color next() {
            Color color = Color.getHSBColor(this.hue, this.saturation, this.brightness);
            this.hue += this.hueStep;
            return color;
        }

    }

    public static class AnimatedRainbow extends Rainbow {

        public AnimatedRainbow(int totalColors, float saturation, float brightness, int speed) {
            super(totalColors, saturation, brightness);

            this.hue = (float) ((((Math.floor(System.currentTimeMillis() / 50.0)) / 360) * speed) % 1);
        }

    }

}
