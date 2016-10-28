package fr.lteconsulting.vroom;

import java.io.IOException;

// TODO visualiser les parties qu'il fait...
// visualiser:
// - une entree du réseau
// - une sortie du réseau
// - manipuler l'entrée à la souris et voir la sortie
// - enregistrer la base de connaissance = liste de [ board + likes ]
// - charger un fichier ds la base de connaissance
// - entrainer le réseau avec la base de connaissance
// - laisser jouer le réseau tout seul et générer dans la base de connaissance
// - enregistrer le réseau de neurones
// - consulter une partie ?
public class App
{
	public static void main( String[] args ) throws IOException
	{
		new LinesApplication().run();
		// new VroomApplication().run();
		// new Vroom2Application().run();
	}

}
