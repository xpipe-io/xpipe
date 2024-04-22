## Midlertidige beholdere

Dette vil køre en midlertidig container ved hjælp af det angivne image, som automatisk vil blive fjernet, når den stoppes. Containeren bliver ved med at køre, selvom der ikke er angivet nogen kommando i containerbilledet, som vil køre.

Dette kan være nyttigt, hvis du hurtigt vil opsætte et bestemt miljø ved hjælp af et bestemt container-image. Du kan så gå ind i containeren som normalt i XPipe, udføre dine operationer og stoppe containeren, når der ikke længere er brug for den. Den bliver så fjernet automatisk.