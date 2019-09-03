# Creating a new recolorable SVG icon

### From Scratch:

1.  Open `webofneeds\webofneeds\won-owner-webapp\src\main\webapp\images\won-icons\` and pick an existing SVG icon with the correct size.
2.  Copy the SVG file and rename it, e.g. to `ico16__new_icon.svg`.
3.  Open the new SVG file with e.g. InkScape.
4.  Change all colors to something searchable, e.g. `#ff11ff`
5.  Create the new SVG icon on top of the old icon using a different searchable color, e.g. `#eeee11`, using the old icon as a size and style reference.
6.  Continue with Step 6 below.


### From an existing SVG:

1.  Find a SVG icon or create a new one using e.g. InkScape. Try to match the style of the existing icons. 
2.  Copy and rename an existing SVG icon in `webofneeds\webofneeds\won-owner-webapp\src\main\webapp\images\won-icons\`.
3.  Open both files with InkScape and change all color to searchable color values, e.g. `#ff11ff` and `#eeee11`. Use different colors for each icon.
4.  Copy the new icon on top of the old icon. 
5.  Resize the new icon to be as big as the old icon. 
6.  Delete all parts of the old icon.
7.  Save the new icon as optimized SVG using `Save as...`.
8.  Open the new SVG file in an Editor (e.g. Visual Studio Code).

**NOTE: if you use VS Code, you can install the `XML Tools` Plugin and auto-format the file with `Shift + Alt + F` for easier editing.**

9.  Search and replace the color value used for creating the new icon, e.g. `#eeee11` with `var(--local-primary)`. Elements that use the color value of the old SVG icon, e.g. `#ff11ff` can be deleted.
10.  Save and add the file to the git repository.
