import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <h2>Portefeuille</h2>
      <p class="muted">
        Suivi des positions et de la watchlist (à implémenter côté back :
        endpoints /portfolio et /watchlist — tables déjà provisionnées
        dans la migration V2).
      </p>
      <p class="muted">
        Fonctionnalités prévues :
      </p>
      <ul class="muted">
        <li>Liste des positions avec P&L non réalisé</li>
        <li>Watchlist avec dernière clôture + variation</li>
        <li>Allocation sectorielle (donut chart)</li>
        <li>Signaux filtrés sur les positions détenues</li>
      </ul>
    </div>
  `,
  styles: [`h2 { margin-top: 0; }`]
})
export class PortfolioComponent {}
