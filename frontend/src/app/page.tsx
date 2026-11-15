import Image from 'next/image';

const categories = ['New Arrivals', 'Indian Wear', 'Western Wear', 'Dresses', 'Everyday Essentials'];

export default function HomePage() {
  return (
    <main>
      <div className="announcement">A considered wardrobe for every day</div>
      <header className="site-header">
        <a className="wordmark" href="/" aria-label="D and Z Fashion home">
          <Image src="/brand/dz-wordmark.svg" alt="D and Z Fashion" width={172} height={54} priority />
        </a>
        <nav className="main-nav" aria-label="Main navigation">
          {categories.map((category) => <a href="#collections" key={category}>{category}</a>)}
        </nav>
        <div className="header-actions">
          <a href="/login">Account</a>
          <a href="/login" aria-label="Sign in to access your shopping bag">Bag</a>
        </div>
      </header>

      <section className="hero" aria-labelledby="hero-title">
        <div className="hero-copy">
          <p className="eyebrow">THE D &amp; Z EDIT</p>
          <h1 id="hero-title">Style that feels<br /><em>like your own.</em></h1>
          <p className="hero-description">A thoughtful edit of women’s and girls’ clothing, made for the moments that make up your day.</p>
          <a className="text-link" href="#collections">Explore the collection <span aria-hidden="true">↗</span></a>
        </div>
        <div className="hero-art" role="img" aria-label="Editorial abstract artwork placeholder for the D and Z seasonal fashion campaign">
          <div className="hero-art-frame">
            <span className="art-index">D&amp;Z / 001</span>
            <div className="art-shape art-shape-one" />
            <div className="art-shape art-shape-two" />
            <span className="art-caption">A NEW PERSPECTIVE<br />ON EVERYDAY DRESSING</span>
          </div>
        </div>
      </section>

      <section className="collection-section" id="collections">
        <div className="section-heading">
          <div><p className="eyebrow">FIND YOUR FEEL</p><h2>Explore the edit</h2></div>
          <p>Thoughtfully grouped. Easy to make your own.</p>
        </div>
        <div className="collection-links">
          {categories.map((category, index) => (
            <a href="/collections" className="collection-link" key={category}>
              <span className="collection-number">0{index + 1}</span>
              <span>{category}</span>
              <span className="collection-arrow" aria-hidden="true">↗</span>
            </a>
          ))}
        </div>
      </section>
      <footer className="site-footer"><span>© D &amp; Z Fashion</span><span>Made for the way you move.</span></footer>
    </main>
  );
}
