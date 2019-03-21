

A few notes about the implementation of the weather widget:

This directory contains a yahoo weather widget, which is based on
the FX weather widget. The UI of this widget is slightly different
than the FX widget.  

1) RC Events for forecast and entering the zip code:

The forecast information is obtained by pressing the down or right arrow 
button.

The left arrow key can be used to erase the zip code displayed in
the left upper corner. After entering the desired code, press the
enter key.

2) Note about the weather icon: Yahoo API's RSS feed contains a pointer to the
weather image in the CDATA section. That's an URL to a gif image. The
Yahoo desktop however, uses a png image of larger size and resolution,
that makes it look better.
Yahoo APIs does not provide access to this image. A different URL
is used to get these images from. I'm not certain about the reliability
of this URL ( it is found from google search). Please pay attention to
the consistency of the image w.r.t to the current weather condition of the
given location.

This application uses the following URL:

Day time images: http://us.i1.yimg.com/us.yimg.com/i/us/nws/weather/gr/{code}d.png
Night time images: http://us.i1.yimg.com/us.yimg.com/i/us/nws/weather/gr/{code}{d|n}.png

In future, if the above URL doesn't point to right images (w.r.t Yahoo APIs)
The workaround is to either use a gif image from the feed. or, the images can
be downloaded and stored on the local disk. The maximum number of
images is going to be 47.

3) Finally, please ensure that the machine on which this
application is run has the correct data and time settings.
