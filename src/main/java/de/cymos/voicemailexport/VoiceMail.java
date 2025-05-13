package de.cymos.voicemailexport;

import java.util.Date;

public record VoiceMail(Date dateSent, String transcript, String caller, Date callDate) { }