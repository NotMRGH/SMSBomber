package ir.mrsf.smsbomber.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class StringUtil {

    public String setPlaceHolder(String phoneNumber, String pattern) {
        pattern = pattern.replaceAll("%phone_number%", phoneNumber);

        final Matcher matcher = Pattern.compile("%phone_number_([\\d<>]+)%").matcher(pattern);
        final StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            final StringBuilder extractedDigits = new StringBuilder();
            final Matcher digitMatcher = Pattern.compile("<(\\d+)>").matcher(matcher.group(1));

            while (digitMatcher.find()) {
                int index = Integer.parseInt(digitMatcher.group(1)) - 1;

                if (index >= 0 && index < phoneNumber.length()) {
                    extractedDigits.append(phoneNumber.charAt(index));
                } else {
                    throw new IllegalArgumentException("Index out of range for phone number.");
                }
            }
            matcher.appendReplacement(result, extractedDigits.toString());
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
