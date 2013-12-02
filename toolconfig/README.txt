EXPORTING:
To export IntelliJ settings, go to 
  File->Export Settings... 
and select these options:
* Code Folding Settings, ...
* Code style schemes, ...
* Default project settings

select the jar file in this folder and overwrite it. 
Next, delete all but project.default.xml from the 'options' folder inside the jar (it contains local paths).
Then delete the unpacked folder (created from the previous version) and unpack the new jar file.



IMPORTING: 
To import settings, go to 
  File->Import Settings...
and select the settings you like
