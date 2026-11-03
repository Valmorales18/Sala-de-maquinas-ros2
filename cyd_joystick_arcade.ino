/*
  CYD (ESP32-2432S028) - Joystick táctil ESTILO ARCADE para ROS 2 / Turtlesim
  Serial 115200, ~20 Hz:  F = adelante | B = atrás | L = izquierda | R = derecha | S = stop

  Librerías: TFT_eSPI (Bodmer) y XPT2046_Touchscreen (Paul Stoffregen)

  User_Setup.h de TFT_eSPI se debe incluir:
    ILI9341_2_DRIVER, pines 12/13/14/15/2, TFT_BL 21,
    USE_HSPI_PORT, LOAD_GLCD, LOAD_FONT2, LOAD_FONT4
*/

#include <SPI.h>
#include <TFT_eSPI.h>
#include <XPT2046_Touchscreen.h>

// ---------- Pines del táctil XPT2046 (CYD) ----------
#define XPT2046_IRQ  36   // (no se usa en el constructor, ver abajo)
#define XPT2046_MOSI 32
#define XPT2046_MISO 39
#define XPT2046_CLK  25
#define XPT2046_CS   33
#define TFT_BL_PIN   21

TFT_eSPI tft = TFT_eSPI();
SPIClass touchSPI(VSPI);
XPT2046_Touchscreen ts(XPT2046_CS);   // sin IRQ: más fiable en la CYD

// ---------- CALIBRACIÓN TÁCTIL (AJUSTAR AQUÍ) ----------
#define TOUCH_X_MIN 200
#define TOUCH_X_MAX 3700
#define TOUCH_Y_MIN 240
#define TOUCH_Y_MAX 3800
#define INVERT_X false
#define INVERT_Y false
#define SWAP_XY  false
#define SCREEN_ROTATION 1     // si cambias esto, cambia también la del táctil (ya va en setup)

#define SCREEN_W 320
#define SCREEN_H 240

// ---------- Estilo ----------
#define COL_BG        TFT_BLACK
#define COL_BTN_DARK  0x0861      // gris muy oscuro azulado (fondo de botón en reposo)
#define COL_TITLE     0x07FF      // cian
#define COL_TITLE_LN  0x0410      // cian oscuro
#define COL_STOP      TFT_RED

#define HUB_X 160
#define HUB_Y 131

// ---------- Botones ----------
enum Dir { DIR_NONE = 0, DIR_UP, DIR_DOWN, DIR_LEFT, DIR_RIGHT };

struct Button {
  int16_t x, y, w, h;
  const char* label;   // texto que se muestra en el centro
  char cmd;            // carácter enviado por Serial
  Dir dir;
  uint16_t color;      // color neón del botón
};

Button buttons[4] = {
  { 115,  28, 90, 62, "FWD",   'F', DIR_UP,    0x07FF },  // cian
  { 115, 172, 90, 62, "BACK",  'B', DIR_DOWN,  0xFD20 },  // naranja
  {  15, 100, 90, 62, "LEFT",  'L', DIR_LEFT,  0xF81F },  // magenta
  { 215, 100, 90, 62, "RIGHT", 'R', DIR_RIGHT, 0xFFE0 }   // amarillo
};

Dir currentDir  = DIR_NONE;
Dir previousDir = DIR_NONE;

// ---------- Utilidades de dibujo ----------
uint16_t dimColor(uint16_t c) { return (c >> 1) & 0x7BEF; }   // ~50% de brillo

void drawArrow(const Button &b, uint16_t color) {
  int cx = b.x + b.w / 2;
  int cy = b.y + b.h / 2;
  const int s = 15;
  switch (b.dir) {
    case DIR_UP:    tft.fillTriangle(cx, cy - s, cx - s, cy + s, cx + s, cy + s, color); break;
    case DIR_DOWN:  tft.fillTriangle(cx, cy + s, cx - s, cy - s, cx + s, cy - s, color); break;
    case DIR_LEFT:  tft.fillTriangle(cx - s, cy, cx + s, cy - s, cx + s, cy + s, color); break;
    case DIR_RIGHT: tft.fillTriangle(cx + s, cy, cx - s, cy - s, cx - s, cy + s, color); break;
    default: break;
  }
}

void drawButton(const Button &b, bool pressed) {
  // Limpia el botón y su zona de brillo
  tft.fillRect(b.x - 4, b.y - 4, b.w + 8, b.h + 8, COL_BG);

  if (pressed) {
    // Efecto "glow" exterior
    uint16_t glow = dimColor(b.color);
    tft.drawRoundRect(b.x - 3, b.y - 3, b.w + 6, b.h + 6, 12, glow);
    tft.drawRoundRect(b.x - 2, b.y - 2, b.w + 4, b.h + 4, 11, glow);
    // Cuerpo relleno de color + marco interior blanco + flecha oscura
    tft.fillRoundRect(b.x, b.y, b.w, b.h, 10, b.color);
    tft.drawRoundRect(b.x + 4, b.y + 4, b.w - 8, b.h - 8, 6, TFT_WHITE);
    drawArrow(b, COL_BG);
  } else {
    // Reposo: fondo oscuro, borde neón doble, flecha de color
    tft.fillRoundRect(b.x, b.y, b.w, b.h, 10, COL_BTN_DARK);
    tft.drawRoundRect(b.x, b.y, b.w, b.h, 10, b.color);
    tft.drawRoundRect(b.x + 1, b.y + 1, b.w - 2, b.h - 2, 9, b.color);
    drawArrow(b, b.color);
  }
}

// Indicador central: muestra el comando que se está enviando
void drawHub(Dir d) {
  uint16_t color = COL_STOP;
  char letter[2] = { 'S', 0 };
  const char* label = "STOP";

  for (int i = 0; i < 4; i++) {
    if (buttons[i].dir == d) {
      color = buttons[i].color;
      letter[0] = buttons[i].cmd;
      label = buttons[i].label;
    }
  }

  tft.fillCircle(HUB_X, HUB_Y, 35, COL_BG);
  tft.drawCircle(HUB_X, HUB_Y, 32, color);
  tft.drawCircle(HUB_X, HUB_Y, 31, color);

  tft.setTextDatum(MC_DATUM);
  tft.setTextColor(color, COL_BG);
  tft.drawString(letter, HUB_X, HUB_Y - 7, 4);
  tft.drawString(label,  HUB_X, HUB_Y + 14, 2);
}

void drawTitle() {
  tft.setTextDatum(MC_DATUM);
  tft.setTextColor(COL_TITLE, COL_BG);
  tft.drawString("ROS 2  TURTLE PAD", SCREEN_W / 2, 11, 2);
  tft.drawFastHLine(0, 23, SCREEN_W, COL_TITLE_LN);
}

void drawAll() {
  tft.fillScreen(COL_BG);
  drawTitle();
  for (int i = 0; i < 4; i++) drawButton(buttons[i], false);
  drawHub(DIR_NONE);
}

// Redibuja SOLO lo que cambió (sin flicker)
void updateVisual(Dir oldDir, Dir newDir) {
  for (int i = 0; i < 4; i++) {
    if (buttons[i].dir == oldDir) drawButton(buttons[i], false);
    if (buttons[i].dir == newDir) drawButton(buttons[i], true);
  }
  drawHub(newDir);
}

// ---------- Táctil ----------
bool getTouchPoint(int16_t &sx, int16_t &sy) {
  if (!ts.touched()) return false;

  TS_Point p = ts.getPoint();
  int32_t rx = p.x;
  int32_t ry = p.y;
  if (SWAP_XY) { int32_t t = rx; rx = ry; ry = t; }

  sx = map(rx, TOUCH_X_MIN, TOUCH_X_MAX, 0, SCREEN_W);
  sy = map(ry, TOUCH_Y_MIN, TOUCH_Y_MAX, 0, SCREEN_H);
  if (INVERT_X) sx = SCREEN_W - sx;
  if (INVERT_Y) sy = SCREEN_H - sy;

  sx = constrain(sx, 0, SCREEN_W - 1);
  sy = constrain(sy, 0, SCREEN_H - 1);
  return true;
}

Dir detectButton(int16_t x, int16_t y) {
  for (int i = 0; i < 4; i++) {
    const Button &b = buttons[i];
    if (x >= b.x && x <= b.x + b.w && y >= b.y && y <= b.y + b.h) return b.dir;
  }
  return DIR_NONE;
}

char dirToCmd(Dir d) {
  for (int i = 0; i < 4; i++) if (buttons[i].dir == d) return buttons[i].cmd;
  return 'S';
}

// ---------- Setup ----------
void setup() {
  Serial.begin(115200);

  pinMode(TFT_BL_PIN, OUTPUT);
  digitalWrite(TFT_BL_PIN, HIGH);

  tft.init();
  tft.setRotation(SCREEN_ROTATION);

  touchSPI.begin(XPT2046_CLK, XPT2046_MISO, XPT2046_MOSI, XPT2046_CS);
  ts.begin(touchSPI);
  ts.setRotation(SCREEN_ROTATION);

  drawAll();
}

// ---------- Loop ----------
void loop() {
  int16_t x = 0, y = 0;
  Dir newDir = DIR_NONE;

  if (getTouchPoint(x, y)) {
    newDir = detectButton(x, y);
  }

  if (newDir != currentDir) {
    previousDir = currentDir;
    currentDir  = newDir;
    updateVisual(previousDir, currentDir);
  }

  // Salida limpia para ROS: un solo carácter por línea, ~20 Hz
  Serial.println(dirToCmd(currentDir));
  delay(50);
}
