#include <SoftwareSerial.h>

#define LED_PIN 2
#define SENSOR_ANALOG_PIN A0
#define BLUETOOTH_RX_PIN 10
#define BLUETOOTH_TX_PIN 11
#define BOOT_BLINK_COUNTER 5

// Configure bluetooth serial communication
SoftwareSerial bluetoothSerial(BLUETOOTH_RX_PIN, BLUETOOTH_TX_PIN); // RX, TX

int sensorValue = 0;
 
void setup() {
  /* set up pin modes */
  pinMode(LED_PIN, OUTPUT);  
   
  /* Debug console init */ 
  Serial.begin(57600);
 
  /* Configure BT baud speed - HC-06
   * only suppport 9600 */
  bluetoothSerial.begin(9600);
  bluetoothSerial.println("I'm ready");
  
  /* boot blink procedure */ 
  for (int i = 0; i < BOOT_BLINK_COUNTER; i++) {
    led_on();
    delay(100);
    led_off();
    delay(100);
  }
   
  Serial.println("Ready to go");  
 
}
 
void loop() {
  /* read sensor value and normalize to a value 0 -- 255 */
  sensorValue = analogRead(SENSOR_ANALOG_PIN) / 4;
  
  /* Some debugging infos */
  Serial.print("Read from sensor value :");
  Serial.println(sensorValue);  
  
  /* print value via bluetooth */
  bluetoothSerial.println(sensorValue);
 
  /* wait for an input from bluetooth channel */
  /* 1 - switch led on  */
  /* 0 - switch led off */
  if (bluetoothSerial.available() > 0) {
    char inChar = bluetoothSerial.read();
    if (inChar == 1) {
        led_on();
    } else if (inChar == 0) {
        led_off();
    }
  }
  delay(1000);
 
}

void led_on() {
    digitalWrite(LED_PIN, HIGH);
}

void led_off() {
    digitalWrite(LED_PIN, LOW);
}
