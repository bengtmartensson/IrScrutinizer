The following appears to work for Ken Eisner <019769e@acadiau.ca>
-----
under a windows platform we can check for lost of DSR and it is
consistant however,  under linux it is not consistant.  We rarely and
infrequently get loss of DSR.
-----
 We found a way around the inconsistant
 detection / loss of DSR.  We decided to check the DSR status in the
 OUTPUT_BUFFER_EMPTY event (I think that is where we put it).
-----
Trent, here is the code which works for us.

Excuse the formating.. damned thing won't space things properly...

Let me know if you need anything else

-Ken

 /**
  * Procedure:   void serialEvent(SerialPortEvent event)
  * Params:    SerialPortEvent event
  *
  * Define what we are doing when one of these events are fired
 **/
  public void serialEvent(SerialPortEvent event)
  {
    int numBytes = 0;
    int offset = 0;

    switch(event.getEventType())
    {
      case SerialPortEvent.BI:
      case SerialPortEvent.OE:
      case SerialPortEvent.FE:
      case SerialPortEvent.PE:
      case SerialPortEvent.CD:
      case SerialPortEvent.RI:
      case SerialPortEvent.CTS:
        break;
      case SerialPortEvent.DSR:
          break;
      case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
        /*
         * HACK: This is designed to work around the inconsistent loss of
DSR
         *       and inconsistent detection of DSR if program running before
         *       cable connected
         *
         * Here we check if the DSR flag is high.
         *   If it is then we check to see if our class var is alread true,
         *     meaning that we have a connection.
         *     If our class var got_dsr is false then we set our class var
         *       got_dsr to true, lost_dsr to false
         *     Else the we already have a connection so just set the class
var
         *       got_dsr to true
         *   Else we check if we already have DSR
         *     If we do then we set the lost_dsr to true, and got_dsr to
false
         *     meaning that we lost DSR
         */
        if (serial_port_.isDSR() == true)
        {
           if (got_dsr == false) /* new connection */
           {
              got_dsr = true;
              System.out.println("\nReceived DSR -- BEGIN OF
TRANSMISSION\n");
              lost_dsr = false;
           }
           else /* old connection */
               got_dsr = true;
        }
        else
        {
           /* already got connection */
           if (got_dsr == true)
           {
      /*
       * Check if we have a bad call
       *
       * 1.) Get the current time (in milliseconds)
       * 2.) If the current time is less than the time of the last
       *     character read then we have major problems
       * 3.) Otherwise, if the current time less the time of the last
       *     char read is less than the length of the end tone then we
       *     have a bad call.
       */
           Date now = new Date();
           long current_time = now.getTime();
           if (current_time <= time_of_last_char)
                System.err.println("CommReader() - Current time is before
last char");
          else
          {
              if ((current_time - time_of_last_char) <= end_tone_length_)
              {
                  System.out.println("\nInterrupted transmission");
                  this.passed_client_checks_ = false;
              }
              else
              {
                  System.out.println("\nGood transmission");
                  this.passed_client_checks_ = true;
              }
      }

      /*
       * Stick a fork in us, we're done here.
       */
       sender_.close_zipfile();

      /*
       * Set lost_dsr to true and got_dsr to false
       * Set finished_receiving_data to true
       */
            lost_dsr = true;
            System.out.println("Lost DSR -- END OF TRANSMISSION");
            got_dsr = false;
      this.finished_receiving_data_ = true;
          }
        }
        break;
      case SerialPortEvent.DATA_AVAILABLE:
        byte[] readBuffer = new byte[2048];

        try
        {
      /*
      * Read while there is something to read, stop when done
      */
          while (is_.available() > 0)
          {
            offset = 0;
            numBytes = is_.read(readBuffer);
      Date now = new Date();
      time_of_last_char = now.getTime();

            if (numBytes > 0)
            {
        /*
        * The first character detected is some weird ^@ thing which we
        * want to ignore so we set the offset to 1 and reduce the
        * numBytes read by one
        */
              if (readBuffer[0] == 0)
        {
                offset = 1;
                numBytes--;
              }
              System.out.print(new String (readBuffer , offset, numBytes));
       sender_.write(readBuffer, offset, numBytes);
            }
          }
        }
        catch (Exception e)
        {
          System.err.println(e.getMessage());
          e.printStackTrace();
        }
        break;
    } /* end switch */
  }

