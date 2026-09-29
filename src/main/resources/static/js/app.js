/* ==========================================================================
   WaveHub — comportamento das páginas logadas
   1. Player único no rodapé (visor VFD): o ▶ de cada linha sintoniza aqui.
   2. Estrela de favorita sem recarregar a página (recarregar pararia a rádio).
   ========================================================================== */
(() => {
    'use strict';

    // ======================================================================
    // 1. PLAYER
    // ======================================================================

    const dock = document.querySelector('.dock');
    const audio = dock && dock.querySelector('[data-dock-audio]');

    if (dock && audio) {
        const ui = {
            toggle: dock.querySelector('[data-dock-toggle]'),
            art: dock.querySelector('[data-dock-art]'),
            status: dock.querySelector('[data-dock-status]'),
            nameBox: dock.querySelector('.vfd-name'),
            name: dock.querySelector('[data-dock-name]'),
            meta: dock.querySelector('[data-dock-meta]'),
            volume: dock.querySelector('[data-dock-volume]')
        };

        const fallbackArt = ui.art.getAttribute('src');
        const STATUS = {
            connecting: 'Conectando…',
            live: 'Ao vivo',
            stopped: 'Parado',
            error: 'Sem sinal · tente outra estação'
        };

        let current = null; // <li> da estação sintonizada

        const isBusy = () => dock.dataset.state === 'live' || dock.dataset.state === 'connecting';

        function setState(state) {
            dock.dataset.state = state;
            ui.status.textContent = STATUS[state];
            ui.toggle.setAttribute('aria-label', state === 'live' || state === 'connecting' ? 'Parar' : 'Ouvir');

            if (!current) return;
            if (state === 'stopped') {
                delete current.dataset.state;
            } else {
                current.dataset.state = state;
            }
        }

        // "BR · Minas Gerais · AAC · 64k", montado a partir da própria linha
        function describe(row) {
            return ['.cc', '.place-name', '.chip']
                .map(selector => row.querySelector(selector))
                .filter(Boolean)
                .map(element => element.textContent.trim())
                .filter(text => text && text !== '—')
                .join(' · ');
        }

        // Nome maior que o visor: liga a rolagem com a distância exata
        function fitName() {
            ui.nameBox.classList.remove('is-long');
            const overflow = ui.name.scrollWidth - ui.nameBox.clientWidth;

            if (overflow > 0) {
                ui.nameBox.style.setProperty('--marquee-distance', `-${overflow + 16}px`);
                ui.nameBox.style.setProperty('--marquee-time', `${Math.max(6, overflow / 22)}s`);
                ui.nameBox.classList.add('is-long');
            }
        }

        function play(row) {
            if (current && current !== row) {
                delete current.dataset.state;
            }
            current = row;

            const icon = row.querySelector('.station-art img');
            ui.art.src = icon ? (icon.currentSrc || icon.src) : fallbackArt;
            ui.name.textContent = row.dataset.name;
            ui.meta.textContent = describe(row);
            ui.toggle.disabled = false;
            fitName();

            // Rádio é ao vivo: sempre reconecta, em vez de continuar de onde parou
            audio.src = row.dataset.src;
            setState('connecting');
            audio.play().catch(error => {
                if (error.name !== 'AbortError') setState('error');
            });
        }

        function stop() {
            audio.pause();
            audio.removeAttribute('src');
            audio.load(); // fecha a conexão com o stream
            setState('stopped');
        }

        document.addEventListener('click', event => {
            const button = event.target.closest('.play');
            if (!button) return;

            const row = button.closest('.station');
            if (row === current && isBusy()) {
                stop();
            } else {
                play(row);
            }
        });

        ui.toggle.addEventListener('click', () => {
            if (!current) return;
            if (isBusy()) {
                stop();
            } else {
                play(current);
            }
        });

        // Eventos do <audio>; ignorados depois do stop() (sem src)
        audio.addEventListener('playing', () => setState('live'));
        audio.addEventListener('waiting', () => {
            if (audio.getAttribute('src')) setState('connecting');
        });
        audio.addEventListener('error', () => {
            if (audio.getAttribute('src')) setState('error');
        });
        audio.addEventListener('ended', () => setState('stopped'));

        // Volume lembrado entre visitas (só neste navegador)
        try {
            const saved = localStorage.getItem('wavehub-volume');
            if (saved !== null) ui.volume.value = saved;
        } catch (error) {
            // navegador sem localStorage: fica o volume padrão
        }
        audio.volume = ui.volume.value / 100;

        ui.volume.addEventListener('input', () => {
            audio.volume = ui.volume.value / 100;
            try {
                localStorage.setItem('wavehub-volume', ui.volume.value);
            } catch (error) {
                // sem localStorage: o volume vale só nesta página
            }
        });

        window.addEventListener('resize', () => {
            if (current) fitName();
        });
    }

    // ======================================================================
    // 2. ESTRELA DE FAVORITA
    // ======================================================================

    const counter = document.querySelector('[data-fav-count]');

    document.addEventListener('submit', async event => {
        const form = event.target.closest('.fav-form');
        if (!form) return;
        event.preventDefault();

        const button = form.querySelector('.fav');
        const row = form.closest('.station');
        button.disabled = true;

        try {
            const response = await fetch(form.dataset.api, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form)), // inclui o token CSRF
                headers: { Accept: 'application/json' }
            });

            const isJson = (response.headers.get('Content-Type') || '').includes('application/json');
            if (!response.ok || !isJson) throw new Error('Resposta inesperada');

            const { favorite } = await response.json();
            const name = row.dataset.name;

            row.classList.toggle('is-fav', favorite);
            button.setAttribute('aria-pressed', String(favorite));
            button.setAttribute('aria-label', favorite
                ? `Remover ${name} das favoritas`
                : `Adicionar ${name} às favoritas`);

            button.classList.remove('just-toggled');
            void button.offsetWidth; // reinicia a animação
            button.classList.add('just-toggled');

            if (counter) {
                counter.textContent = Math.max(0, Number(counter.textContent) + (favorite ? 1 : -1));
            }
        } catch (error) {
            // Sessão expirada ou falha de rede: envia o formulário do jeito tradicional
            form.submit();
        } finally {
            button.disabled = false;
        }
    });
})();
