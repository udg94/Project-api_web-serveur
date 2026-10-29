package com.example.projectapi.record;

import java.time.LocalDateTime;

public record MessageForm (Long expediteur_id, Long destination_id,String expediteur_username, String contenu, LocalDateTime date){}
