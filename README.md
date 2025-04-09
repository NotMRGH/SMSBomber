## Features
| Feature              | Supported |
|----------------------|:---------:|
| Multithreading       |     ✅     |
| Proxy Support        |     ✅     |
| Custom API Support   |     ✅     |
| WordPress Scanner    |     ✅     |
| Phone Login Detector |     ✅     |

## Tools

### 🔍 WordPress Scanner
This tool checks whether a given website is built with **WordPress** by looking for typical WordPress markers such as:
- Presence of `/wp-content/`, `/wp-includes/` in page HTML
- `meta[name=generator]` tag with `WordPress`
- Cookies starting with `wp-` or `wordpress_`
- Existence of `https://api.w.org/` link tag

It helps you identify potential WordPress targets for more focused scanning.

### 📱 Phone Login Detector
This tool scans a website to detect if it uses **phone number-based authentication**, by:
- Crawling public pages like `/login`, `/register`, `/my-account/`, etc.
- Searching page content for keywords like `موبایل`, `شماره`, `mobile number`, `phone number`, `OTP`, etc.
- Accepts **custom keyword input** from the user for flexible detection

Useful for filtering WordPress sites that use mobile login forms.

## Installation
1. Ensure that **Java 17** is installed on your device.
2. Download the ZIP file from the [**Releases**](https://github.com/NotMRGH/SMSBomber/releases) section.
3. Extract the contents and add any additional APIs as needed.

## Running the Application
To run the application, navigate to the directory where the `SMSBomber.jar` file is located and execute the following command:

```shell
~/SMSBomber java -jar SMSBomber.jar
```

## Example Config
For a POST request:
```json
{
  "name": "Example Name",
  "url": "https://example.com",
  "method": "POST",
  "withOutZero": false,
  "repeat": 1,
  "payload": {
    "cellphone": "%phone_number%"
  }
}
```

Alternatively:
```json
{
  "name": "Example Name",
  "url": "https://example.com",
  "method": "POST",
  "withOutZero": false,
  "repeat": 1,
  "payload": "cellphone: %phone_number%"
}
```

For a GET request:
```json
{
  "name": "Example Name",
  "url": "https://example.com/?phone=%phone_number%",
  "method": "GET",
  "withOutZero": false,
  "repeat": 1
}
```

## Notes:
If you want to remove the leading zero from the phone number, set the following option to `true`:
```
"withOutZero": true
```

If you want to forcibly set `ContentType`, add the following option:
```
"forceContentType": "application/json"
```
```
"forceContentType": "application/x-www-form-urlencoded"
```

## PlaceHolders:
Default phone number format:
```
%phone_number%
```

Customized phone number format (e.g., inserting - between digits):
```
%phone_number_<1><2><3><4>%-%phone_number_<5><6><7>%-%phone_number_<8><9><10><11>%
```
Example output:
```
0911-000-0000
```

Alternatively:
```
%phone_number_<1><2><3><4>% TEST %phone_number_<8><9><10>%
```
Example output:
```
0911 TEST 000
```