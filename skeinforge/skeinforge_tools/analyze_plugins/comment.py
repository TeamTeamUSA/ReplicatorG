"""
Comment is a script to comment a gcode file.

The default 'Activate Comment' checkbox is on.  When it is on, the functions described below will work when called from the skeinforge toolchain, when it is off, the functions will not be called from the toolchain.  The functions will still be called, whether or not the 'Activate Comment' checkbox is on, when comment is run directly.

To run comment, in a shell in the folder which comment is in type:
> python comment.py

An explanation of the gcodes is at:
http://reprap.org/bin/view/Main/Arduino_GCode_Interpreter

and at:
http://reprap.org/bin/view/Main/MCodeReference

A gode example is at:
http://forums.reprap.org/file.php?12,file=565

This example comments the gcode file Screw Holder_comb.gcode.  This example is run in a terminal in the folder which contains Screw Holder_comb.gcode and comment.py.


> python
Python 2.5.1 (r251:54863, Sep 22 2007, 01:43:31)
[GCC 4.2.1 (SUSE Linux)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> import comment
>>> comment.main()
This brings up the comment dialog.


>>> comment.commentFile()
The commented file is saved as Screw Holder_comb_comment.gcode

"""

from __future__ import absolute_import
#Init has to be imported first because it has code to workaround the python bug where relative imports don't work if the module is imported as a main module.
import __init__

from skeinforge_tools.skeinforge_utilities import gcodec
from skeinforge_tools.skeinforge_utilities import preferences
from skeinforge_tools.meta_plugins import polyfile
import cStringIO
import sys


__author__ = "Enrique Perez (perez_enrique@yahoo.com)"
__date__ = "$Date: 2008/21/04 $"
__license__ = "GPL 3.0"


def commentFile( fileName = '' ):
	"Comment a gcode file.  If no fileName is specified, comment the first gcode file in this folder that is not modified."
	gcodeText = gcodec.getFileText( fileName )
	writeCommentFileGivenText( fileName, gcodeText )

def getCommentGcode( gcodeText ):
	"Get gcode text with added comments."
	skein = CommentSkein()
	skein.parseGcode( gcodeText )
	return skein.output.getvalue()

def getRepositoryConstructor():
	"Get the repository constructor."
	return CommentPreferences()

def writeCommentFileGivenText( fileName, gcodeText ):
	"Write a commented gcode file for a gcode file."
	gcodec.writeFileMessageEnd( '_comment.gcode', fileName, getCommentGcode( gcodeText ), 'The commented file is saved as ' )

def writeOutput( fileName, gcodeText = '' ):
	"Write a commented gcode file for a skeinforge gcode file, if 'Write Commented File for Skeinforge Chain' is selected."
	commentPreferences = CommentPreferences()
	preferences.getReadRepository( commentPreferences )
	if gcodeText == '':
		gcodeText = gcodec.getFileText( fileName )
	if commentPreferences.activateComment.value:
		writeCommentFileGivenText( fileName, gcodeText )


class CommentSkein:
	"A class to comment a gcode skein."
	def __init__( self ):
		self.oldLocation = None
		self.output = cStringIO.StringIO()

	def addComment( self, comment ):
		"Add a gcode comment and a newline to the output."
		self.output.write( "( " + comment + " )\n" )

	def linearMove( self, splitLine ):
		"Comment a linear move."
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		self.addComment( "Linear move to " + str( location ) + "." );
		self.oldLocation = location

	def parseGcode( self, gcodeText ):
		"Parse gcode text and store the commented gcode."
		lines = gcodec.getTextLines( gcodeText )
		for line in lines:
			self.parseLine( line )

	def parseLine( self, line ):
		"Parse a gcode line and add it to the commented gcode."
		splitLine = line.split()
		if len( splitLine ) < 1:
			return
		firstWord = splitLine[ 0 ]
		if firstWord == 'G1':
			self.linearMove( splitLine )
		elif firstWord == 'G2':
			self.setHelicalMoveEndpoint( splitLine )
			self.addComment( "Helical clockwise move to " + str( self.oldLocation ) + "." )
		elif firstWord == 'G3':
			self.setHelicalMoveEndpoint( splitLine )
			self.addComment( "Helical counterclockwise move to " + str( self.oldLocation ) + "." )
		elif firstWord == 'G21':
			self.addComment( "Set units to mm." )
		elif firstWord == 'G28':
			self.addComment( "Start at home." )
		elif firstWord == 'G90':
			self.addComment( "Set positioning to absolute." )
		elif firstWord == 'M101':
			self.addComment( "Extruder on, forward." );
		elif firstWord == 'M102':
			self.addComment( "Extruder on, reverse." );
		elif firstWord == 'M103':
			self.addComment( "Extruder off." )
		elif firstWord == 'M104':
			self.addComment( "Set temperature to " + str( gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] ) ) + " C." )
		elif firstWord == 'M105':
			self.addComment( "Custom code for temperature reading." )
		elif firstWord == 'M106':
			self.addComment( "Turn fan on." )
		elif firstWord == 'M107':
			self.addComment( "Turn fan off." )
		elif firstWord == 'M108':
			self.addComment( "Set extruder speed to " + str( gcodec.getDoubleAfterFirstLetter( splitLine[ 1 ] ) ) + "." )
		self.output.write( line + "\n" )

	def setHelicalMoveEndpoint( self, splitLine ):
		"Get the endpoint of a helical move."
		if self.oldLocation == None:
			print( "A helical move is relative and therefore must not be the first move of a gcode file." )
			return
		location = gcodec.getLocationFromSplitLine( self.oldLocation, splitLine )
		location += self.oldLocation
		self.oldLocation = location


class CommentPreferences:
	"A class to handle the comment preferences."
	def __init__( self ):
		"Set the default preferences, execute title & preferences fileName."
		#Set the default preferences.
		preferences.addListsToRepository( self )
		self.activateComment = preferences.BooleanPreference().getFromValue( 'Activate Comment', self, False )
		self.fileNameInput = preferences.Filename().getFromFilename( [ ( 'Gcode text files', '*.gcode' ) ], 'Open File to Write Comments for', self, '' )
		#Create the archive, title of the execute button, title of the dialog & preferences fileName.
		self.executeTitle = 'Write Comments'
		self.saveCloseTitle = 'Save and Close'
		preferences.setHelpPreferencesFileNameTitleWindowPosition( self, 'skeinforge_tools.analyze_plugins.comment.html' )

	def execute( self ):
		"Write button has been clicked."
		fileNames = polyfile.getFileOrGcodeDirectory( self.fileNameInput.value, self.fileNameInput.wasCancelled, [ '_comment' ] )
		for fileName in fileNames:
			commentFile( fileName )


def main():
	"Display the comment dialog."
	if len( sys.argv ) > 1:
		writeOutput( ' '.join( sys.argv[ 1 : ] ) )
	else:
		preferences.startMainLoopFromConstructor( getRepositoryConstructor() )

if __name__ == "__main__":
	main()

