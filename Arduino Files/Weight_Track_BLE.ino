#include <SoftwareSerial.h>
#include <Time.h>
#include <TimeAlarms.h>
#include <HX711.h>
#include <stdlib.h>

#define DEBUG 1

//#define WEIGHT_READ_PIN 12 //digital input pin to read ADC
//#define WEIGHT_SCK_PIN 13 //digital output pin to control ADC SCK
#define CALIBRATION_FACTOR -7000

#define WEIGHT_EN_PIN 9 //digital output enable the actual weighing circuit
#define RISE_TIME 10    //at most ~10 ms for Vcc of peripherals to stabilize

//#define DEFAULT_WEIGHT_INTERVAL 28800 //28800 seconds = 8 hours, measure weight every 8 hours by default
#define DEFAULT_WEIGHT_INTERVAL 20 //measure weight every 20 seconds for demo purposes
#define DEFAULT_BUF_THRESH 5  //5 weight values buffer

SoftwareSerial bluetooth(10, 11); // RX, TX
HX711 scale(12, 13); //DOUT, SCK

//general buffer variables in loop
byte int_buf = 0;
char * str_buf;
float float_buf = 0;

//system operation variables
boolean pe_mode = 0; //0 - normal (high-energy) mode, 1 - power saving mode
unsigned int delay_time = 1000; //~1000 in high energy mode, ~10000 in power saving mode

byte send_buf = DEFAULT_BUF_THRESH; //max number of bytes held in buffer
float *weight_buf; //weight data buffer
byte weight_buf_index = 0; //current number of bytes in weight data buffer

AlarmId weight_id;


void setup()
{
  // Setup Pins
  pinMode(WEIGHT_EN_PIN, OUTPUT); //for duty cycling the weighing circuit for energy saving
  digitalWrite(WEIGHT_EN_PIN, LOW);
  
            if(DEBUG)
            {
              pinMode(13, OUTPUT); //built-in LED
              digitalWrite(13, LOW);
            }


            // Open serial communications and wait for port to open:
            if (DEBUG)
            {
              Serial.begin(9600);
              
              while (!Serial) {
                ; // wait for serial port to connect. Needed for native USB port only
              }
            
              Serial.println("Arduino - Serial Monitor Connected");
            }

  // set up the ADC
  scale.set_scale(CALIBRATION_FACTOR);
  scale.tare();
  
  // set the bluetooth SoftwareSerial connection
  bluetooth.begin(9600);
  Alarm.delay(1000);
  Serial.println("Bluetooth Connected");

            if (DEBUG && false)
            {
              bluetooth.print("AT\r\n");
              bluetooth.print("AT+NAMEWEIGHTTRAK\r\n");
              bluetooth.print("AT+PIN06285\r\n");
            }
  // setup default weighing interval to timer mode
  weight_buf = (float*) malloc(send_buf * sizeof(float));
  weight_id = Alarm.timerRepeat(DEFAULT_WEIGHT_INTERVAL, measure_weight);
  Alarm.enable(weight_id);
}

void loop()
{
  int_buf = 0;
  int_buf = receive_data();

  //Alarm.delay(delay_time);
}


void send_buf_data(byte send_buf_index)
{
  byte i = 0;
  
  //"wwww-mm/dd/yyyy:hh:mm:ss\r\n"
  while (i < send_buf_index)
  {
    bluetooth.print(weight_buf[i]);
    bluetooth.print("\r\n");

    i++;

            if (DEBUG)
            {
              Serial.print(weight_buf[i]);
          Serial.print("\r\n");
            }
  }

  //reset weight buffer index
  weight_buf_index = 0;
  
  return;
}

byte receive_data()
{
  if (bluetooth.available() > 1) //wait for more than 2 bytes of data to be received (header (1 byte) + data (most likely type int))
  {
    unsigned int i = 0;
    char *rec_buf = (char*) malloc((bluetooth.available()) * sizeof(char));
    
    char c;
    c = bluetooth.read();

    Serial.print(c);
    
    if (c == 'A') //AT commands - change module name, etc.
    {
      ; //to be filled at a later date
    }

    else if (c == 'M') //Measure Weight - Mx, measure x times
    {
      int x = bluetooth.parseInt();
      weight_buf_index = 0;
      weight_buf = realloc(weight_buf, x * sizeof(float));
      
 Serial.println(x);
      
      for (int i = 0; i < x; i++)
      {
        measure_weight();
      }

      send_buf_data(x);
    }

    else if (c == 'D') //Daily Alarm commands - set Alarm to alarmRepeat, along with when
    {
      Alarm.disable(weight_id);
      Alarm.free(weight_id);


      i = bluetooth.parseInt();
      weight_id = Alarm.alarmRepeat((int)(i/10000), (i%10000) - (i%100), (i%100), measure_weight);
      Alarm.enable(weight_id);
    }

    else if (c == 'I') //Interval commands - set Alarm to timerRepeat, along with its interval
    {
      Alarm.disable(weight_id);
      Alarm.free(weight_id);

      i = bluetooth.parseInt();
      weight_id = Alarm.timerRepeat(i, measure_weight);
      Alarm.enable(weight_id);
    }
    
    else if (c == 'P') //Power commands - turn power saving mode on or off
    {
      i = bluetooth.parseInt();
      if (i) //i = 1: turn on power-energy mode
        pe_mode = 1;
      else
        pe_mode = 0;
    }

    else if (c == 'S') //set send_buf - set amount of data to be sent at once
    {
      i = bluetooth.parseInt();
      send_buf = i;
    }

    else
      return 1; //error - header not recognized0


    free(rec_buf);
  }

  return 0; //no error
}

void measure_weight()
{
  digitalWrite(WEIGHT_EN_PIN, HIGH);  //turn on weighing circuit
  Alarm.delay(RISE_TIME);  //wait for voltage to stabilize

  while (ADC_read() < 0) //make sure measured weight is positive or 0
    weight_buf[weight_buf_index] = ADC_read();  //read ADC data
  weight_buf_index++;

  Serial.print ("Measure Weight - ");
  Serial.print (weight_buf_index);
  Serial.print (" ");
  Serial.println (weight_buf[weight_buf_index]);
  
  digitalWrite(WEIGHT_EN_PIN, LOW); //turn off weighing circuit
  
  return;
}

long ADC_read()
{
  long weight = 0;
  
  weight = scale.get_units();
  
  return weight;
}



